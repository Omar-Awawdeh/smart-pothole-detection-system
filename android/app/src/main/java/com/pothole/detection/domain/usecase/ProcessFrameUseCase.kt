package com.pothole.detection.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.location.Location
import com.pothole.detection.data.local.PendingUpload
import com.pothole.detection.data.repository.PotholeRepository
import com.pothole.detection.deduplication.PotholeDeduplicator
import com.pothole.detection.detection.DetectionResult
import com.pothole.detection.detection.PotholeDetector
import com.pothole.detection.detection.drawDetectionsOnBitmap
import com.pothole.detection.location.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ProcessFrameUseCase @Inject constructor(
    private val detector: PotholeDetector,
    private val deduplicator: PotholeDeduplicator,
    private val locationProvider: LocationProvider,
    private val repository: PotholeRepository,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val appContext: Context
) {
    data class ProcessResult(
        val detections: List<DetectionResult>,
        val reportedCount: Int,
        val queuedUploadIds: List<String>,
        val location: Location?,
        val inferenceTimeMs: Long,
        val maxConfidence: Float,
        val candidatesAboveThreshold: Int,
        val keptAfterNms: Int,
        val delegate: String
    )

    suspend fun execute(bitmap: Bitmap, confidenceThreshold: Float = 0.5f): ProcessResult {
        val debugResult = detector.detectWithDebug(bitmap, confidenceThreshold)
        val detections = debugResult.detections
        val location = locationProvider.getLastLocation()
        var reportedCount = 0
        val queuedUploadIds = mutableListOf<String>()

        if (detections.isNotEmpty() && location != null) {
            val vehicleId = sharedPreferences.getString("vehicle_id", "22222222-0000-0000-0000-000000000001") ?: "22222222-0000-0000-0000-000000000001"

            for (detection in detections) {
                if (deduplicator.shouldReport(location.latitude, location.longitude)) {
                    reportedCount++

                    val timestamp = System.currentTimeMillis()
                    val annotatedBitmap = drawDetectionsOnBitmap(bitmap, detections)
                    val imageFile = saveFrameAsJpeg(annotatedBitmap, timestamp)
                    val upload = PendingUpload(
                        localImagePath = imageFile.absolutePath,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        confidence = detection.confidence,
                        vehicleId = vehicleId,
                        timestamp = timestamp
                    )
                    repository.queueUpload(upload)
                    queuedUploadIds.add(upload.id)
                }
            }
        }

        val inferenceTime = debugResult.debugInfo.inferenceTimeMs

        return ProcessResult(
            detections = detections,
            reportedCount = reportedCount,
            queuedUploadIds = queuedUploadIds,
            location = location,
            inferenceTimeMs = inferenceTime,
            maxConfidence = debugResult.debugInfo.maxConfidence,
            candidatesAboveThreshold = debugResult.debugInfo.candidatesAboveThreshold,
            keptAfterNms = debugResult.debugInfo.keptAfterNms,
            delegate = debugResult.debugInfo.delegate
        )
    }

    private fun saveFrameAsJpeg(bitmap: Bitmap, timestamp: Long): File {
        val dir = File(appContext.filesDir, "pothole_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "pothole_$timestamp.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file
    }
}
