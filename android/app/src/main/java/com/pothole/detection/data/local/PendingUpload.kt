package com.pothole.detection.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_uploads")
data class PendingUpload(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val localImagePath: String,
    val latitude: Double,
    val longitude: Double,
    val confidence: Float,
    val vehicleId: String,
    val timestamp: Long,
    val failureCount: Int = 0
)
