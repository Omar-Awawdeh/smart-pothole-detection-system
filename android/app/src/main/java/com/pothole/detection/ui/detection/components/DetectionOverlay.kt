package com.pothole.detection.ui.detection.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.pothole.detection.detection.DetectionResult

@Composable
fun DetectionOverlay(
    detections: List<DetectionResult>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val scaleX = size.width / imageWidth.toFloat()
        val scaleY = size.height / imageHeight.toFloat()

        for (detection in detections) {
            val box = detection.boundingBox
            val left = box.left * scaleX
            val top = box.top * scaleY
            val width = box.width() * scaleX
            val height = box.height() * scaleY

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 3f)
            )

            val label = "Pothole ${(detection.confidence * 100).toInt()}%"
            val textLayoutResult = textMeasurer.measure(
                text = label,
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top - textLayoutResult.size.height),
                size = Size(
                    textLayoutResult.size.width.toFloat() + 8f,
                    textLayoutResult.size.height.toFloat()
                )
            )

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(left + 4f, top - textLayoutResult.size.height),
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )
        }
    }
}
