package dev.flare.speedtest.ui.components

import java.util.Locale

/** Speeds read best with fewer decimals as they grow: 0.42, 9.87, 42.1, 940. */
fun formatSpeed(value: Double, locale: Locale = Locale.getDefault()): String = when {
    value < 10.0 -> String.format(locale, "%.2f", value)
    value < 100.0 -> String.format(locale, "%.1f", value)
    else -> String.format(locale, "%.0f", value)
}

fun formatMillis(value: Double, locale: Locale = Locale.getDefault()): String =
    if (value < 10.0) String.format(locale, "%.1f", value) else String.format(locale, "%.0f", value)
