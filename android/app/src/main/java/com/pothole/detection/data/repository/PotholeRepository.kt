package com.pothole.detection.data.repository

import com.pothole.detection.data.local.PendingUpload
import com.pothole.detection.data.local.PendingUploadDao
import com.pothole.detection.network.ApiService
import com.pothole.detection.network.models.PotholeResponse
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class PotholeRepository @Inject constructor(
    private val dao: PendingUploadDao,
    private val apiService: ApiService
) {
    val pendingUploads: Flow<List<PendingUpload>> = dao.getAll()
    val pendingCount: Flow<Int> = dao.getCount()

    suspend fun queueUpload(upload: PendingUpload) = dao.insert(upload)

    suspend fun uploadAndRemove(upload: PendingUpload): Result<PotholeResponse> {
        val imageFile = java.io.File(upload.localImagePath)
        if (!imageFile.exists()) {
            dao.delete(upload)
            return Result.failure(Exception("Image file not found"))
        }

        val imageBytes = imageFile.readBytes()
        val result = apiService.uploadPothole(
            imageBytes = imageBytes,
            latitude = upload.latitude,
            longitude = upload.longitude,
            confidence = upload.confidence,
            vehicleId = upload.vehicleId,
            timestamp = upload.timestamp
        )

        if (result.isSuccess) {
            dao.delete(upload)
            imageFile.delete()
        }

        return result
    }

    suspend fun incrementFailureCount(upload: PendingUpload) {
        dao.update(upload.copy(failureCount = upload.failureCount + 1))
    }

    suspend fun removeUpload(upload: PendingUpload) {
        dao.delete(upload)
        java.io.File(upload.localImagePath).delete()
    }
}
