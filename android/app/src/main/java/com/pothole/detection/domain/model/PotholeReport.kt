package com.pothole.detection.domain.model

data class PotholeReport(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val confidence: Float,
    val localImagePath: String,
    val timestamp: Long,
    val uploaded: Boolean = false,
    val failureCount: Int = 0
)
