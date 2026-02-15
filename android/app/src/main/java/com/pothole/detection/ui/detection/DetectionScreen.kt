package com.pothole.detection.ui.detection

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pothole.detection.ui.detection.components.CameraPreview
import com.pothole.detection.ui.detection.components.DetectionOverlay
import com.pothole.detection.ui.detection.components.StatsCard

@Composable
fun DetectionScreen(
    viewModel: DetectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onCameraPermissionResult(
            permissions[Manifest.permission.CAMERA] == true
        )
        viewModel.onLocationPermissionResult(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        )
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    if (!state.cameraPermissionGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Camera permission is required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Grant Permissions")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            isAnalyzing = state.isDetecting,
            frameSkipRate = state.frameSkipRate,
            onFrameAnalyzed = { bitmap ->
                viewModel.processFrame(bitmap)
            }
        )

        if (state.recentDetections.isNotEmpty()) {
            DetectionOverlay(
                detections = state.recentDetections,
                imageWidth = state.lastFrameWidth,
                imageHeight = state.lastFrameHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCard(
                location = state.currentLocation,
                detectionsToday = state.detectionsToday,
                inferenceTimeMs = state.inferenceTimeMs,
                pendingUploads = state.pendingUploads,
                maxConfidence = state.maxConfidence,
                candidatesAboveThreshold = state.candidatesAboveThreshold,
                keptAfterNms = state.keptAfterNms,
                delegate = state.delegate
            )

            Button(
                onClick = {
                    if (state.isDetecting) {
                        viewModel.stopDetection()
                    } else {
                        viewModel.startDetection()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isDetecting) Color.Red
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state.isDetecting) "STOP DETECTION" else "START DETECTION",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
