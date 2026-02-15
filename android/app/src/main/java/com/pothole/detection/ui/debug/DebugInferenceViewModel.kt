package com.pothole.detection.ui.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pothole.detection.detection.DetectionResult
import com.pothole.detection.detection.PotholeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DebugInferenceUiState(
    val assetPath: String = "debug_frames/frame_01.jpg",
    val confidenceThreshold: Float = 0.5f,
    val isRunning: Boolean = false,
    val bitmap: Bitmap? = null,
    val detections: List<DetectionResult> = emptyList(),
    val debugInfoText: String = "",
    val errorText: String = ""
)

@HiltViewModel
class DebugInferenceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: PotholeDetector
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

        _uiState.update { it.copy(isRunning = true, errorText = "") }
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
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        bitmap = null,
                        detections = emptyList(),
                        debugInfoText = "",
                        errorText = t.message ?: t::class.java.simpleName
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
}
