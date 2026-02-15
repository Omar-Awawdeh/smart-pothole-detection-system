package com.pothole.detection.ui.detection

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pothole.detection.detection.DetectionResult
import com.pothole.detection.domain.usecase.ProcessFrameUseCase
import com.pothole.detection.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val inferenceTimeMs: Long = 0,
    val maxConfidence: Float = 0f,
    val candidatesAboveThreshold: Int = 0,
    val keptAfterNms: Int = 0,
    val delegate: String = "",
    val confidenceThreshold: Float = 0.5f,
    val frameSkipRate: Int = 2,
    val cameraPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false
)

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val processFrameUseCase: ProcessFrameUseCase,
    private val locationProvider: LocationProvider,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    private var isProcessingFrame = false

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val confidence = sharedPreferences.getFloat(PREF_CONFIDENCE_THRESHOLD, 0.5f)
        val frameSkip = sharedPreferences.getInt(PREF_FRAME_SKIP_RATE, 2)
        _uiState.update {
            it.copy(
                confidenceThreshold = confidence.coerceIn(0.01f, 1.0f),
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
        _uiState.update { it.copy(isDetecting = false, recentDetections = emptyList()) }
    }

    fun processFrame(bitmap: Bitmap) {
        if (isProcessingFrame) return
        isProcessingFrame = true

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = processFrameUseCase.execute(
                    bitmap = bitmap,
                    confidenceThreshold = _uiState.value.confidenceThreshold
                )

                _uiState.update { state ->
                    state.copy(
                        recentDetections = result.detections,
                        lastFrameWidth = bitmap.width,
                        lastFrameHeight = bitmap.height,
                        inferenceTimeMs = result.inferenceTimeMs,
                        maxConfidence = result.maxConfidence,
                        candidatesAboveThreshold = result.candidatesAboveThreshold,
                        keptAfterNms = result.keptAfterNms,
                        delegate = result.delegate,
                        currentLocation = result.location ?: state.currentLocation,
                        detectionsToday = state.detectionsToday + result.reportedCount
                    )
                }
            } finally {
                isProcessingFrame = false
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
        private const val PREF_FRAME_SKIP_RATE = "frame_skip_rate"
    }
}
