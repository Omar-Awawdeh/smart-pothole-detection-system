package com.pothole.detection.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pothole.detection.ui.detection.components.DetectionOverlay
import kotlin.math.roundToInt

@Composable
fun DebugInferencePanel(
    viewModel: DebugInferenceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Debug Inference (assets)",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = uiState.assetPath,
                onValueChange = viewModel::updateAssetPath,
                label = { Text("Asset path") },
                supportingText = { Text("Example: debug_frames/frame_01.jpg (under android/app/src/main/assets/)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Confidence Threshold: ${(uiState.confidenceThreshold * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = uiState.confidenceThreshold,
                onValueChange = viewModel::updateConfidenceThreshold,
                valueRange = 0.01f..1.0f
            )

            Button(
                onClick = viewModel::runOnce,
                enabled = !uiState.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isRunning) "RUNNING..." else "RUN INFERENCE")
            }

            if (uiState.errorText.isNotBlank()) {
                Text(
                    text = uiState.errorText,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.debugInfoText.isNotBlank()) {
                Text(
                    text = uiState.debugInfoText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val bitmap = uiState.bitmap
            if (bitmap != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Debug frame",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    DetectionOverlay(
                        detections = uiState.detections,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
