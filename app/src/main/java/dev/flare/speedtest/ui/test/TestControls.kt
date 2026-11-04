package dev.flare.speedtest.ui.test

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.label

@Composable
fun StopButton(onStop: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onStop, modifier = modifier.height(52.dp)) {
        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.test_stop), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun DoneButtons(onAgain: () -> Unit, onShare: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onAgain, modifier = Modifier.weight(1f).height(52.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.test_again), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
        }
        FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f).height(52.dp)) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.action_share), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Returns a click handler that opens the system share sheet with a readable summary. */
@Composable
fun rememberShare(measurement: Measurement?, unit: SpeedUnit): () -> Unit {
    val context = LocalContext.current
    if (measurement == null) return {}
    val unitLabel = unit.label()
    val text = buildString {
        append(
            stringResource(
                R.string.share_text,
                formatSpeed(unit.fromMbps(measurement.downloadMbps)),
                formatSpeed(unit.fromMbps(measurement.uploadMbps)),
                unitLabel,
                formatMillis(measurement.pingMs),
                formatMillis(measurement.jitterMs),
            ),
        )
        val info = measurement.info
        val provider = listOfNotNull(info?.isp, info?.serverCity ?: info?.serverCode).joinToString(" · ")
        if (provider.isNotBlank()) append("\n").append(provider)
    }
    val title = stringResource(R.string.action_share)
    return { context.share(text, title) }
}

private fun Context.share(text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, title))
}
