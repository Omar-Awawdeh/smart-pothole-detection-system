package com.pothole.detection.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pothole.detection.data.local.PendingUpload
import com.pothole.detection.data.local.PendingUploadDao
import com.pothole.detection.worker.UploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UploadEntryState(
    val upload: PendingUpload,
    val workStatus: String = "Unknown"
)

data class HistoryUiState(
    val entries: List<UploadEntryState> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val pendingUploadDao: PendingUploadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch {
            pendingUploadDao.getAll().collect { uploads ->
                val entries = uploads.map { upload ->
                    val status = getWorkStatus(upload.id)
                    UploadEntryState(upload = upload, workStatus = status)
                }
                _uiState.update {
                    it.copy(entries = entries, isLoading = false)
                }
            }
        }
    }

    private fun getWorkStatus(uploadId: String): String {
        val workName = "upload_$uploadId"
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(workName).get()
            if (workInfos.isNullOrEmpty()) {
                "Not Scheduled"
            } else {
                val info = workInfos.first()
                when (info.state) {
                    WorkInfo.State.ENQUEUED -> "Queued"
                    WorkInfo.State.RUNNING -> "Uploading..."
                    WorkInfo.State.SUCCEEDED -> "Uploaded"
                    WorkInfo.State.FAILED -> "Failed"
                    WorkInfo.State.BLOCKED -> "Blocked"
                    WorkInfo.State.CANCELLED -> "Cancelled"
                }
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun retryUpload(upload: PendingUpload) {
        UploadWorker.enqueue(context, upload.id)
        refreshStatus(upload.id)
    }

    fun deleteUpload(upload: PendingUpload) {
        viewModelScope.launch {
            workManager.cancelUniqueWork("upload_${upload.id}")
            pendingUploadDao.delete(upload)
            val imageFile = File(upload.localImagePath)
            if (imageFile.exists()) {
                imageFile.delete()
            }
        }
    }

    private fun refreshStatus(uploadId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val updatedEntries = currentState.entries.map { entry ->
                    if (entry.upload.id == uploadId) {
                        entry.copy(workStatus = getWorkStatus(uploadId))
                    } else {
                        entry
                    }
                }
                currentState.copy(entries = updatedEntries)
            }
        }
    }
}
