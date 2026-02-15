package com.pothole.detection.deduplication

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PotholeDeduplicator(
    private val dedupRadiusMeters: Double = DEDUP_RADIUS_METERS,
    private val dedupTimeWindowMs: Long = DEDUP_TIME_WINDOW_MS,
    private val maxRecords: Int = MAX_RECORDS
) {
    private val recentDetections = mutableListOf<DetectionRecord>()

    data class DetectionRecord(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long
    )

    @Synchronized
    fun shouldReport(latitude: Double, longitude: Double): Boolean {
        val currentTime = System.currentTimeMillis()

        recentDetections.removeAll { record ->
            currentTime - record.timestamp > dedupTimeWindowMs
        }

        for (record in recentDetections) {
            val distance = haversineDistance(
                latitude, longitude,
                record.latitude, record.longitude
            )
            if (distance < dedupRadiusMeters) {
                return false
            }
        }

        recentDetections.add(DetectionRecord(latitude, longitude, currentTime))

        if (recentDetections.size > maxRecords) {
            recentDetections.removeAt(0)
        }

        return true
    }

    fun clear() {
        recentDetections.clear()
    }

    companion object {
        private const val DEDUP_RADIUS_METERS = 10.0
        private const val DEDUP_TIME_WINDOW_MS = 60_000L
        private const val MAX_RECORDS = 100
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        fun haversineDistance(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_METERS * c
        }
    }
}
