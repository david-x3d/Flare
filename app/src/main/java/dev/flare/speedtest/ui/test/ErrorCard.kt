package dev.flare.speedtest.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedTestError

@Composable
fun SpeedTestError.message(): String = when (this) {
    SpeedTestError.Offline -> stringResource(R.string.error_offline)
    is SpeedTestError.Unreachable -> stringResource(R.string.error_unreachable, host)
    SpeedTestError.Timeout -> stringResource(R.string.error_timeout)
    is SpeedTestError.EndpointChanged -> stringResource(
        R.string.error_endpoint_changed,
        url,
        httpCode?.let { "HTTP $it" } ?: detail ?: stringResource(R.string.error_no_detail),
    )
    is SpeedTestError.Unknown -> stringResource(R.string.error_unknown, message ?: stringResource(R.string.error_no_detail))
}

@Composable
fun ErrorCard(error: SpeedTestError, onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    if (error == SpeedTestError.Offline) Icons.Rounded.WifiOff else Icons.Rounded.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Text(error.message(), style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss)) }
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}
