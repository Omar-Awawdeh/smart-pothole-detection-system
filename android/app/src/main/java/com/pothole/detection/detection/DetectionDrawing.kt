package com.pothole.detection.detection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

fun drawDetectionsOnBitmap(
    bitmap: Bitmap,
    detections: List<DetectionResult>
): Bitmap {
    val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    val bgPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    for (detection in detections) {
        val box = detection.boundingBox

        canvas.drawRect(box, boxPaint)

        val label = "Pothole ${(detection.confidence * 100).toInt()}%"
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.textSize

        canvas.drawRect(
            box.left,
            box.top - textHeight - 4f,
            box.left + textWidth + 12f,
            box.top,
            bgPaint
        )
        canvas.drawText(label, box.left + 6f, box.top - 4f, textPaint)
    }

    return output
}
