package com.pothole.detection.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.max
import kotlin.math.min

class PotholeDetector(context: Context) : AutoCloseable {
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .add(CastOp(DataType.FLOAT32))
        .build()

    private val nnApiDelegate: NnApiDelegate?
    private val gpuDelegate: GpuDelegate?
    private val interpreter: Interpreter
    private val delegateLabel: String

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE_NAME)

        var loadedNnApiDelegate: NnApiDelegate? = null
        var loadedGpuDelegate: GpuDelegate? = null
        var loadedDelegateLabel = "CPU"

        val loadedInterpreter = run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    loadedNnApiDelegate = NnApiDelegate()
                    val nnApiOptions = Interpreter.Options().apply {
                        numThreads = NUM_THREADS
                        addDelegate(loadedNnApiDelegate)
                    }
                    loadedDelegateLabel = "NNAPI"
                    return@run Interpreter(modelBuffer, nnApiOptions)
                } catch (_: Throwable) {
                    loadedNnApiDelegate?.close()
                    loadedNnApiDelegate = null
                }
            }

            try {
                loadedGpuDelegate = GpuDelegate()
                val gpuOptions = Interpreter.Options().apply {
                    numThreads = NUM_THREADS
                    addDelegate(loadedGpuDelegate)
                }
                loadedDelegateLabel = "GPU"
                return@run Interpreter(modelBuffer, gpuOptions)
            } catch (_: Throwable) {
                loadedGpuDelegate?.close()
                loadedGpuDelegate = null
            }

            val cpuOptions = Interpreter.Options().apply {
                numThreads = NUM_THREADS
            }
            loadedDelegateLabel = "CPU"
            return@run Interpreter(modelBuffer, cpuOptions)
        }

        interpreter = loadedInterpreter
        nnApiDelegate = loadedNnApiDelegate
        gpuDelegate = loadedGpuDelegate
        delegateLabel = loadedDelegateLabel
        interpreter.allocateTensors()
    }

    fun detect(bitmap: Bitmap, confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD): List<DetectionResult> {
        return detectWithDebug(bitmap, confidenceThreshold).detections
    }

    data class DebugInfo(
        val maxConfidence: Float,
        val maxConfidenceIndex: Int,
        val candidatesAboveThreshold: Int,
        val keptAfterNms: Int,
        val delegate: String,
        val inferenceTimeMs: Long
    )

    data class DebugDetectionResult(
        val detections: List<DetectionResult>,
        val debugInfo: DebugInfo
    )

    fun detectWithDebug(
        bitmap: Bitmap,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): DebugDetectionResult {
        val inferenceStartMs = SystemClock.elapsedRealtime()

        val inputImage = TensorImage(DataType.FLOAT32)
        inputImage.load(bitmap)
        val processedImage = imageProcessor.process(inputImage)

        val output = Array(1) { Array(OUTPUT_CHANNELS) { FloatArray(NUM_PREDICTIONS) } }
        interpreter.run(processedImage.buffer, output)

        val confidenceArray = output[0][4]
        var maxConfidence = -Float.MAX_VALUE
        var maxConfidenceIndex = 0
        for (i in 0 until NUM_PREDICTIONS) {
            val c = confidenceArray[i]
            if (c > maxConfidence) {
                maxConfidence = c
                maxConfidenceIndex = i
            }
        }

        val candidates = ArrayList<Pair<RectF, Float>>()
        for (predictionIndex in 0 until NUM_PREDICTIONS) {
            val confidence = confidenceArray[predictionIndex]
            if (confidence < confidenceThreshold) {
                continue
            }

            val xCenterNorm = output[0][0][predictionIndex]
            val yCenterNorm = output[0][1][predictionIndex]
            val widthNorm = output[0][2][predictionIndex]
            val heightNorm = output[0][3][predictionIndex]

            val xCenter = xCenterNorm * INPUT_SIZE
            val yCenter = yCenterNorm * INPUT_SIZE
            val width = widthNorm * INPUT_SIZE
            val height = heightNorm * INPUT_SIZE

            val halfWidth = width / 2f
            val halfHeight = height / 2f

            val left = max(0f, xCenter - halfWidth)
            val top = max(0f, yCenter - halfHeight)
            val right = min(INPUT_SIZE.toFloat(), xCenter + halfWidth)
            val bottom = min(INPUT_SIZE.toFloat(), yCenter + halfHeight)

            if (right <= left || bottom <= top) {
                continue
            }

            candidates.add(RectF(left, top, right, bottom) to confidence)
        }

        val keptIndices = NmsProcessor.apply(candidates, NMS_IOU_THRESHOLD)
        val inferenceTimeMs = SystemClock.elapsedRealtime() - inferenceStartMs

        val scaleX = bitmap.width.toFloat() / INPUT_SIZE.toFloat()
        val scaleY = bitmap.height.toFloat() / INPUT_SIZE.toFloat()

        val detections = keptIndices.map { index ->
            val (box640, confidence) = candidates[index]
            val scaledBox = RectF(
                box640.left * scaleX,
                box640.top * scaleY,
                box640.right * scaleX,
                box640.bottom * scaleY
            )
            DetectionResult(
                boundingBox = scaledBox,
                confidence = confidence,
                inferenceTimeMs = inferenceTimeMs
            )
        }

        return DebugDetectionResult(
            detections = detections,
            debugInfo = DebugInfo(
                maxConfidence = maxConfidence,
                maxConfidenceIndex = maxConfidenceIndex,
                candidatesAboveThreshold = candidates.size,
                keptAfterNms = keptIndices.size,
                delegate = delegateLabel,
                inferenceTimeMs = inferenceTimeMs
            )
        )
    }

    override fun close() {
        interpreter.close()
        gpuDelegate?.close()
        nnApiDelegate?.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "best_float16.tflite"
        private const val INPUT_SIZE = 640
        private const val OUTPUT_CHANNELS = 5
        private const val NUM_PREDICTIONS = 8400
        private const val NUM_THREADS = 4
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
        private const val NMS_IOU_THRESHOLD = 0.5f
    }
}
