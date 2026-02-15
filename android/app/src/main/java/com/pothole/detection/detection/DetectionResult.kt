package com.pothole.detection.detection

import android.graphics.RectF

data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float,
    val inferenceTimeMs: Long
)
