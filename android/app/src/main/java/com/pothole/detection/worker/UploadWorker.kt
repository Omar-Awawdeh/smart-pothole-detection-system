package com.pothole.detection.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pothole.detection.data.local.PendingUploadDao
import com.pothole.detection.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: PendingUploadDao,
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return Result.failure()
        val pendingUpload = dao.getById(uploadId) ?: return Result.failure()

        val imageFile = File(pendingUpload.localImagePath)
        if (!imageFile.exists()) {
            dao.delete(pendingUpload)
            return Result.failure()
        }

        val authResult = ensureAuthenticated()
        if (authResult != null) return authResult

        val imageBytes = imageFile.readBytes()
        val result = apiService.uploadPothole(
            imageBytes = imageBytes,
            latitude = pendingUpload.latitude,
            longitude = pendingUpload.longitude,
            confidence = pendingUpload.confidence,
            vehicleId = pendingUpload.vehicleId,
            timestamp = pendingUpload.timestamp
        )

        return if (result.isSuccess) {
            dao.delete(pendingUpload)
            imageFile.delete()
            Result.success()
        } else {
            val exception = result.exceptionOrNull()
            if (exception?.message?.contains("401") == true) {
                Log.w(TAG, "Got 401 after auth, retrying with fresh login")
                apiService.clearAccessToken()
                return Result.retry()
            }
            if (isRetryableError(exception)) {
                Result.retry()
            } else {
                val updatedUpload = pendingUpload.copy(failureCount = pendingUpload.failureCount + 1)
                if (updatedUpload.failureCount >= MAX_RETRIES) {
                    dao.delete(pendingUpload)
                    imageFile.delete()
                } else {
                    dao.update(updatedUpload)
                }
                Result.failure()
            }
        }
    }

    private suspend fun ensureAuthenticated(): Result? {
        if (apiService.hasAccessToken()) return null

        val email = sharedPreferences.getString(KEY_AUTH_EMAIL, null)
        val password = sharedPreferences.getString(KEY_AUTH_PASSWORD, null)

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            Log.e(TAG, "No auth credentials configured in settings — cannot upload")
            return Result.failure()
        }

        val loginResult = apiService.login(email, password)
        return if (loginResult.isSuccess) {
            Log.d(TAG, "Authenticated successfully as $email")
            null
        } else {
            Log.e(TAG, "Login failed: ${loginResult.exceptionOrNull()?.message}")
            Result.failure()
        }
    }

    private fun isRetryableError(exception: Throwable?): Boolean {
        if (exception == null) return false
        val message = exception.message ?: return true
        return !message.contains("400") && !message.contains("401") &&
               !message.contains("403") && !message.contains("404") &&
               !message.contains("422")
    }

    companion object {
        private const val TAG = "UploadWorker"
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_AUTH_EMAIL = "auth_email"
        const val KEY_AUTH_PASSWORD = "auth_password"
        private const val MAX_RETRIES = 5

        fun buildWorkRequest(uploadId: String): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(workDataOf(KEY_UPLOAD_ID to uploadId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }

        fun enqueue(context: Context, uploadId: String) {
            val workRequest = buildWorkRequest(uploadId)
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "upload_$uploadId",
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}
