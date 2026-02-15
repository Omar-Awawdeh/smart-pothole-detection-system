package com.pothole.detection.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pothole.detection.data.local.PendingUpload
import com.pothole.detection.data.local.PendingUploadDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val pendingUploads: List<PendingUpload> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val pendingUploadDao: PendingUploadDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pendingUploadDao.getAll().collect { uploads ->
                _uiState.update {
                    it.copy(
                        pendingUploads = uploads,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteUpload(upload: PendingUpload) {
        viewModelScope.launch {
            pendingUploadDao.delete(upload)
            val imageFile = File(upload.localImagePath)
            if (imageFile.exists()) {
                imageFile.delete()
            }
        }
    }
}
