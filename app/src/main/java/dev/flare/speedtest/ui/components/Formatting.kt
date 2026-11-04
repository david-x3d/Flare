package dev.flare.speedtest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedUnit
import java.util.Locale

/** Speeds read best with fewer decimals as they grow: 0.42, 9.87, 42.1, 940. */
fun formatSpeed(value: Double, locale: Locale = Locale.getDefault()): String = when {
    value < 10.0 -> String.format(locale, "%.2f", value)
    value < 100.0 -> String.format(locale, "%.1f", value)
    else -> String.format(locale, "%.0f", value)
}

fun formatMillis(value: Double, locale: Locale = Locale.getDefault()): String =
    if (value < 10.0) String.format(locale, "%.1f", value) else String.format(locale, "%.0f", value)

/** Decimal units, like the payload sizes: 980 kB, 4.8 MB, 48 MB, 1.20 GB. */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String = when {
    bytes < 1_000_000 -> String.format(locale, "%.0f kB", bytes / 1e3)
    bytes < 10_000_000 -> String.format(locale, "%.1f MB", bytes / 1e6)
    bytes < 1_000_000_000 -> String.format(locale, "%.0f MB", bytes / 1e6)
    else -> String.format(locale, "%.2f GB", bytes / 1e9)
}

@Composable
fun SpeedUnit.label(): String = stringResource(
    when (this) {
        SpeedUnit.MBIT_PER_SECOND -> R.string.unit_mbit
        SpeedUnit.MBYTE_PER_SECOND -> R.string.unit_mbyte
    },
)
