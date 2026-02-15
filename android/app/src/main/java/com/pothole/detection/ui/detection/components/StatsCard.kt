package com.pothole.detection.ui.detection.components

import android.location.Location
import com.pothole.detection.BuildConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatsCard(
    location: Location?,
    detectionsToday: Int,
    inferenceTimeMs: Long,
    pendingUploads: Int,
    maxConfidence: Float,
    candidatesAboveThreshold: Int,
    keptAfterNms: Int,
    delegate: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (location != null) {
                        String.format("%.4f, %.4f", location.latitude, location.longitude)
                    } else {
                        "Location unavailable"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Detected today: $detectionsToday",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Inference: ${inferenceTimeMs}ms",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Pending uploads: $pendingUploads",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (BuildConfig.DEBUG) {
                Text(
                    text = "maxConf=%.3f cand=%d kept=%d %s".format(
                        maxConfidence,
                        candidatesAboveThreshold,
                        keptAfterNms,
                        if (delegate.isBlank()) "?" else delegate
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
