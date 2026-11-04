package dev.flare.speedtest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.NetworkType

fun NetworkType.icon(): ImageVector = when (this) {
    NetworkType.WIFI -> Icons.Rounded.Wifi
    NetworkType.CELLULAR -> Icons.Rounded.SignalCellularAlt
    NetworkType.ETHERNET -> Icons.Rounded.Lan
    NetworkType.VPN -> Icons.Rounded.VpnKey
    NetworkType.OTHER -> Icons.Rounded.Public
    NetworkType.OFFLINE -> Icons.Rounded.WifiOff
}

@Composable
fun NetworkType.label(): String = stringResource(
    when (this) {
        NetworkType.WIFI -> R.string.network_wifi
        NetworkType.CELLULAR -> R.string.network_cellular
        NetworkType.ETHERNET -> R.string.network_ethernet
        NetworkType.VPN -> R.string.network_vpn
        NetworkType.OTHER -> R.string.network_other
        NetworkType.OFFLINE -> R.string.network_offline
    },
)

/** Pill with the current connection type and, once known, the provider. */
@Composable
fun NetworkChip(type: NetworkType, modifier: Modifier = Modifier, detail: String? = null) {
    val offline = type == NetworkType.OFFLINE
    Surface(
        modifier = modifier.widthIn(max = 240.dp),
        shape = CircleShape,
        color = if (offline) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (offline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                type.icon(), contentDescription = null, modifier = Modifier.size(16.dp),
                tint = if (offline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
            )
            Text(
                if (detail.isNullOrBlank()) type.label() else "${type.label()} · $detail",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
