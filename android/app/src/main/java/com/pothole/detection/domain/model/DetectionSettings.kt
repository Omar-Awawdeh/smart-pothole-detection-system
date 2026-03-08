package com.pothole.detection.domain.model

data class DetectionSettings(
    val confidenceThreshold: Float = 0.3f,
    val nmsThreshold: Float = 0.45f,
    val frameSkipRate: Int = 2,
    val dedupRadiusMeters: Double = 10.0,
    val dedupTimeWindowMs: Long = 60_000L,
    val imageQuality: Int = 85,
    val apiBaseUrl: String = "https://api.potholesystem.tech"
)
