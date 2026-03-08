package com.pothole.detection.ui.detection

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pothole.detection.detection.DetectionResult
import com.pothole.detection.domain.usecase.ProcessFrameUseCase
import com.pothole.detection.location.LocationProvider
import com.pothole.detection.worker.UploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetectionUiState(
    val isDetecting: Boolean = false,
    val currentLocation: Location? = null,
    val recentDetections: List<DetectionResult> = emptyList(),
    val lastFrameWidth: Int = 1,
    val lastFrameHeight: Int = 1,
    val detectionsToday: Int = 0,
    val pendingUploads: Int = 0,
    val preprocessTimeMs: Long = 0,
    val inferenceTimeMs: Long = 0,
    val postprocessTimeMs: Long = 0,
    val totalTimeMs: Long = 0,
    val maxConfidence: Float = 0f,
    val candidatesAboveThreshold: Int = 0,
    val keptAfterNms: Int = 0,
    val delegate: String = "",
    val confidenceThreshold: Float = 0.3f,
    val nmsThreshold: Float = 0.45f,
    val frameSkipRate: Int = 2,
    val droppedFrames: Int = 0,
    val processedFrames: Int = 0,
    val cameraPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false
)

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val processFrameUseCase: ProcessFrameUseCase,
    private val locationProvider: LocationProvider,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    private var isProcessingFrame = false
    private var pendingFrame: Bitmap? = null

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val confidence = sharedPreferences.getFloat(PREF_CONFIDENCE_THRESHOLD, 0.3f)
        val nmsThreshold = sharedPreferences.getFloat(PREF_NMS_THRESHOLD, 0.45f)
        val frameSkip = sharedPreferences.getInt(PREF_FRAME_SKIP_RATE, 2)
        _uiState.update {
            it.copy(
                confidenceThreshold = confidence.coerceIn(0.01f, 1.0f),
                nmsThreshold = nmsThreshold.coerceIn(0.01f, 1.0f),
                frameSkipRate = frameSkip.coerceIn(1, 10)
            )
        }
    }

    fun startDetection() {
        loadPreferences()
        locationProvider.startLocationUpdates()
        _uiState.update { it.copy(isDetecting = true) }

        viewModelScope.launch {
            locationProvider.currentLocation.collect { location ->
                _uiState.update { it.copy(currentLocation = location) }
            }
        }
    }

    fun stopDetection() {
        locationProvider.stopLocationUpdates()
        pendingFrame = null
        isProcessingFrame = false
        _uiState.update { it.copy(isDetecting = false, recentDetections = emptyList()) }
    }

    fun processFrame(bitmap: Bitmap) {
        if (isProcessingFrame) {
            pendingFrame = bitmap
            _uiState.update { it.copy(droppedFrames = it.droppedFrames + 1) }
            return
        }

        processBitmapInternal(bitmap)
    }

    private fun processBitmapInternal(bitmap: Bitmap) {
        isProcessingFrame = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = processFrameUseCase.execute(
                    bitmap = bitmap,
                    confidenceThreshold = _uiState.value.confidenceThreshold,
                    nmsThreshold = _uiState.value.nmsThreshold
                )

                for (uploadId in result.queuedUploadIds) {
                    UploadWorker.enqueue(appContext, uploadId)
                }

                _uiState.update { state ->
                    state.copy(
                        recentDetections = result.detections,
                        lastFrameWidth = bitmap.width,
                        lastFrameHeight = bitmap.height,
                        preprocessTimeMs = result.preprocessTimeMs,
                        inferenceTimeMs = result.inferenceTimeMs,
                        postprocessTimeMs = result.postprocessTimeMs,
                        totalTimeMs = result.totalTimeMs,
                        maxConfidence = result.maxConfidence,
                        candidatesAboveThreshold = result.candidatesAboveThreshold,
                        keptAfterNms = result.keptAfterNms,
                        delegate = result.delegate,
                        confidenceThreshold = result.confidenceThreshold,
                        nmsThreshold = result.nmsThreshold,
                        currentLocation = result.location ?: state.currentLocation,
                        detectionsToday = state.detectionsToday + result.reportedCount,
                        processedFrames = state.processedFrames + 1
                    )
                }
            } finally {
                isProcessingFrame = false
                val queuedFrame = pendingFrame
                pendingFrame = null
                if (_uiState.value.isDetecting && queuedFrame != null) {
                    processBitmapInternal(queuedFrame)
                }
            }
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopLocationUpdates()
    }

    companion object {
        private const val PREF_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val PREF_NMS_THRESHOLD = "nms_threshold"
        private const val PREF_FRAME_SKIP_RATE = "frame_skip_rate"
    }
}
