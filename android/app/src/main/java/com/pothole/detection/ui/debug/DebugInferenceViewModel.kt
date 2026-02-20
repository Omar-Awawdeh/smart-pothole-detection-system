package com.pothole.detection.ui.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pothole.detection.data.local.PendingUpload
import com.pothole.detection.data.repository.PotholeRepository
import com.pothole.detection.detection.DetectionResult
import com.pothole.detection.detection.PotholeDetector
import com.pothole.detection.worker.UploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DebugInferenceUiState(
    val assetPath: String = "debug_frames/frame_01.jpg",
    val confidenceThreshold: Float = 0.5f,
    val isRunning: Boolean = false,
    val bitmap: Bitmap? = null,
    val detections: List<DetectionResult> = emptyList(),
    val debugInfoText: String = "",
    val errorText: String = "",
    val testUploadStatus: String = ""
)

@HiltViewModel
class DebugInferenceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: PotholeDetector,
    private val repository: PotholeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugInferenceUiState())
    val uiState: StateFlow<DebugInferenceUiState> = _uiState.asStateFlow()

    fun updateAssetPath(value: String) {
        _uiState.update { it.copy(assetPath = value, errorText = "") }
    }

    fun updateConfidenceThreshold(value: Float) {
        _uiState.update { it.copy(confidenceThreshold = value, errorText = "") }
    }

    fun runOnce() {
        val current = _uiState.value
        if (current.isRunning) return

        _uiState.update { it.copy(isRunning = true, errorText = "", testUploadStatus = "") }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromAssets(current.assetPath.trim())
                }

                val result = detector.detectWithDebug(
                    bitmap = bitmap,
                    confidenceThreshold = current.confidenceThreshold
                )

                val info = result.debugInfo
                val debugInfoText = buildString {
                    append("asset=")
                    append(current.assetPath.trim())
                    append("\n")
                    append("bitmap=")
                    append(bitmap.width)
                    append("x")
                    append(bitmap.height)
                    append("\n")
                    append("threshold=")
                    append(current.confidenceThreshold)
                    append("\n")
                    append("maxConf=")
                    append(info.maxConfidence)
                    append(" idx=")
                    append(info.maxConfidenceIndex)
                    append("\n")
                    append("candidates=")
                    append(info.candidatesAboveThreshold)
                    append(" kept=")
                    append(info.keptAfterNms)
                    append("\n")
                    append("delegate=")
                    append(info.delegate)
                    append("\n")
                    append("inferenceMs=")
                    append(info.inferenceTimeMs)
                }

                _uiState.update {
                    it.copy(
                        bitmap = bitmap,
                        detections = result.detections,
                        debugInfoText = debugInfoText,
                        errorText = ""
                    )
                }

                if (result.detections.isEmpty()) {
                    _uiState.update { it.copy(testUploadStatus = "No potholes detected — nothing to upload") }
                    return@launch
                }

                val bestDetection = result.detections.maxBy { it.confidence }
                val timestamp = System.currentTimeMillis()
                val imageFile = withContext(Dispatchers.IO) {
                    saveFrameAsJpeg(bitmap, timestamp)
                }

                val prefs = context.getSharedPreferences("pothole_detection_prefs", Context.MODE_PRIVATE)
                val vehicleId = prefs.getString("vehicle_id", "test-device") ?: "test-device"

                val upload = PendingUpload(
                    localImagePath = imageFile.absolutePath,
                    latitude = 0.0,
                    longitude = 0.0,
                    confidence = bestDetection.confidence,
                    vehicleId = vehicleId,
                    timestamp = timestamp
                )
                repository.queueUpload(upload)
                UploadWorker.enqueue(context, upload.id)

                _uiState.update {
                    it.copy(
                        testUploadStatus = "Pothole detected (${(bestDetection.confidence * 100).toInt()}%) — queued for upload"
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        bitmap = null,
                        detections = emptyList(),
                        debugInfoText = "",
                        errorText = t.message ?: t::class.java.simpleName,
                        testUploadStatus = ""
                    )
                }
            } finally {
                _uiState.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun loadBitmapFromAssets(assetPath: String): Bitmap {
        require(assetPath.isNotBlank()) { "Asset path is empty" }
        context.assets.open(assetPath).use { input ->
            val decoded = BitmapFactory.decodeStream(input)
            return decoded ?: error("Failed to decode bitmap from assets: $assetPath")
        }
    }

    private fun saveFrameAsJpeg(bitmap: Bitmap, timestamp: Long): File {
        val dir = File(context.filesDir, "pothole_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "pothole_$timestamp.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file
    }
}
