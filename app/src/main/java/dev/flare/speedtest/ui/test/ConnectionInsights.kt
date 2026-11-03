package dev.flare.speedtest.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestPhase

@Composable
fun ConnectionInsights(state: TestUiState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.connection_insights), style = MaterialTheme.typography.titleLargeEmphasized)
        if (state.phase != TestPhase.DONE) {
            Text(stringResource(if (state.isRunning) R.string.measuring else R.string.insight_ready), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val down = state.downloadMbps ?: 0.0
            val up = state.uploadMbps ?: 0.0
            val ping = state.pingMs ?: Double.POSITIVE_INFINITY
            val jitter = state.jitterMs ?: Double.POSITIVE_INFINITY
            val rows = listOf(
                Triple(Icons.Rounded.LiveTv, R.string.insight_streaming, down >= 25),
                Triple(Icons.Rounded.VideoCall, R.string.insight_calls, down >= 5 && up >= 3 && ping < 150 && jitter < 30),
                Triple(Icons.Rounded.SportsEsports, R.string.insight_gaming, down >= 10 && up >= 1 && ping < 60 && jitter < 15),
            )
            rows.forEach { (icon, title, good) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(if (good) R.string.insight_good else R.string.insight_limited), color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(stringResource(R.string.insight_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
