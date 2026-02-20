package com.pothole.detection.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Pending Uploads") }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.entries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending uploads",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.entries,
                        key = { it.upload.id }
                    ) { entry ->
                        val upload = entry.upload
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Confidence: ${(upload.confidence * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        StatusChip(status = entry.workStatus)
                                    }
                                    Text(
                                        text = "Lat/Lng: ${upload.latitude}, ${upload.longitude}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = DateFormat.getDateTimeInstance().format(Date(upload.timestamp)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (upload.failureCount > 0) {
                                        Text(
                                            text = "Failed attempts: ${upload.failureCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (entry.workStatus == "Failed" || entry.workStatus == "Cancelled" || entry.workStatus == "Not Scheduled") {
                                        IconButton(onClick = { viewModel.retryUpload(upload) }) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry upload"
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteUpload(upload) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete upload"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(status: String) {
    val chipColor = when (status) {
        "Queued" -> MaterialTheme.colorScheme.secondaryContainer
        "Uploading..." -> MaterialTheme.colorScheme.primaryContainer
        "Uploaded" -> MaterialTheme.colorScheme.tertiaryContainer
        "Failed" -> MaterialTheme.colorScheme.errorContainer
        "Cancelled" -> MaterialTheme.colorScheme.errorContainer
        "Blocked" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (status) {
        "Queued" -> MaterialTheme.colorScheme.onSecondaryContainer
        "Uploading..." -> MaterialTheme.colorScheme.onPrimaryContainer
        "Uploaded" -> MaterialTheme.colorScheme.onTertiaryContainer
        "Failed" -> MaterialTheme.colorScheme.onErrorContainer
        "Cancelled" -> MaterialTheme.colorScheme.onErrorContainer
        "Blocked" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = chipColor
        )
    )
}
