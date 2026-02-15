package com.pothole.detection.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
private const val KEY_FRAME_SKIP_RATE = "frame_skip_rate"
private const val KEY_API_BASE_URL = "api_base_url"
private const val KEY_VEHICLE_ID = "vehicle_id"

data class SettingsUiState(
    val confidenceThreshold: Float = 0.5f,
    val frameSkipRate: Int = 2,
    val apiBaseUrl: String = "https://api.yoursite.com",
    val vehicleId: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            confidenceThreshold = sharedPreferences.getFloat(KEY_CONFIDENCE_THRESHOLD, 0.5f),
            frameSkipRate = sharedPreferences.getInt(KEY_FRAME_SKIP_RATE, 2),
            apiBaseUrl = sharedPreferences.getString(KEY_API_BASE_URL, "https://api.yoursite.com")
                ?: "https://api.yoursite.com",
            vehicleId = sharedPreferences.getString(KEY_VEHICLE_ID, "") ?: ""
        )
    }

    fun updateConfidenceThreshold(value: Float) {
        _uiState.update { it.copy(confidenceThreshold = value) }
        viewModelScope.launch {
            sharedPreferences.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, value).apply()
        }
    }

    fun updateFrameSkipRate(value: Int) {
        _uiState.update { it.copy(frameSkipRate = value) }
        viewModelScope.launch {
            sharedPreferences.edit().putInt(KEY_FRAME_SKIP_RATE, value).apply()
        }
    }

    fun updateApiBaseUrl(value: String) {
        _uiState.update { it.copy(apiBaseUrl = value) }
        viewModelScope.launch {
            sharedPreferences.edit().putString(KEY_API_BASE_URL, value).apply()
        }
    }

    fun updateVehicleId(value: String) {
        _uiState.update { it.copy(vehicleId = value) }
        viewModelScope.launch {
            sharedPreferences.edit().putString(KEY_VEHICLE_ID, value).apply()
        }
    }

}
