package com.pothole.detection.detection

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object NmsProcessor {
    fun apply(
        boxes: List<Pair<RectF, Float>>,
        iouThreshold: Float = 0.5f
    ): List<Int> {
        if (boxes.isEmpty()) {
            return emptyList()
        }

        val sortedIndices = boxes.indices.sortedByDescending { boxes[it].second }.toMutableList()
        val kept = ArrayList<Int>()

        while (sortedIndices.isNotEmpty()) {
            val current = sortedIndices.removeAt(0)
            kept.add(current)

            val currentBox = boxes[current].first
            val remaining = ArrayList<Int>(sortedIndices.size)

            for (candidate in sortedIndices) {
                val candidateBox = boxes[candidate].first
                val iou = computeIou(currentBox, candidateBox)
                if (iou <= iouThreshold) {
                    remaining.add(candidate)
                }
            }

            sortedIndices.clear()
            sortedIndices.addAll(remaining)
        }

        return kept
    }

    private fun computeIou(a: RectF, b: RectF): Float {
        val intersectionLeft = max(a.left, b.left)
        val intersectionTop = max(a.top, b.top)
        val intersectionRight = min(a.right, b.right)
        val intersectionBottom = min(a.bottom, b.bottom)

        val intersectionWidth = max(0f, intersectionRight - intersectionLeft)
        val intersectionHeight = max(0f, intersectionBottom - intersectionTop)
        val intersectionArea = intersectionWidth * intersectionHeight

        val areaA = max(0f, a.width()) * max(0f, a.height())
        val areaB = max(0f, b.width()) * max(0f, b.height())
        val unionArea = areaA + areaB - intersectionArea

        if (unionArea <= 0f) {
            return 0f
        }

        return intersectionArea / unionArea
    }
}
