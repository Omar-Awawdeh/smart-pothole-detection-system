package com.pothole.detection.domain.usecase

import android.graphics.Bitmap
import android.location.Location
import com.pothole.detection.deduplication.PotholeDeduplicator
import com.pothole.detection.detection.DetectionResult
import com.pothole.detection.detection.PotholeDetector
import com.pothole.detection.location.LocationProvider
import javax.inject.Inject

class ProcessFrameUseCase @Inject constructor(
    private val detector: PotholeDetector,
    private val deduplicator: PotholeDeduplicator,
    private val locationProvider: LocationProvider
) {
    data class ProcessResult(
        val detections: List<DetectionResult>,
        val reportedCount: Int,
        val location: Location?,
        val inferenceTimeMs: Long,
        val maxConfidence: Float,
        val candidatesAboveThreshold: Int,
        val keptAfterNms: Int,
        val delegate: String
    )

    fun execute(bitmap: Bitmap, confidenceThreshold: Float = 0.5f): ProcessResult {
        val debugResult = detector.detectWithDebug(bitmap, confidenceThreshold)
        val detections = debugResult.detections
        val location = locationProvider.getLastLocation()
        var reportedCount = 0

        if (detections.isNotEmpty() && location != null) {
            for (detection in detections) {
                if (deduplicator.shouldReport(location.latitude, location.longitude)) {
                    reportedCount++
                }
            }
        }

        val inferenceTime = debugResult.debugInfo.inferenceTimeMs

        return ProcessResult(
            detections = detections,
            reportedCount = reportedCount,
            location = location,
            inferenceTimeMs = inferenceTime,
            maxConfidence = debugResult.debugInfo.maxConfidence,
            candidatesAboveThreshold = debugResult.debugInfo.candidatesAboveThreshold,
            keptAfterNms = debugResult.debugInfo.keptAfterNms,
            delegate = debugResult.debugInfo.delegate
        )
    }
}
