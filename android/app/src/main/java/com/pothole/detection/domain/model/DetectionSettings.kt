package com.pothole.detection.domain.model

data class DetectionSettings(
    val confidenceThreshold: Float = 0.5f,
    val frameSkipRate: Int = 2,
    val dedupRadiusMeters: Double = 10.0,
    val dedupTimeWindowMs: Long = 60_000L,
    val imageQuality: Int = 85,
    val apiBaseUrl: String = "https://api.potholesystem.tech"
)
