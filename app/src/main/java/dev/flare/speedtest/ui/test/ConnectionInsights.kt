package dev.flare.speedtest.ui.test

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.BloatGrade
import dev.flare.speedtest.domain.Grade
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.Quality
import dev.flare.speedtest.domain.UseCase
import dev.flare.speedtest.ui.components.FlareCard
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.components.formatBytes
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import dev.flare.speedtest.ui.theme.NumberStyle

@Composable
fun Grade.color(): Color = when (this) {
    Grade.EXCELLENT, Grade.GOOD -> Flare.palette.good
    Grade.FAIR -> Flare.palette.warn
    Grade.POOR -> Flare.palette.bad
}

private fun Grade.title() = when (this) {
    Grade.EXCELLENT -> R.string.grade_excellent
    Grade.GOOD -> R.string.grade_good
    Grade.FAIR -> R.string.grade_fair
    Grade.POOR -> R.string.grade_poor
}

private fun Grade.body() = when (this) {
    Grade.EXCELLENT -> R.string.grade_body_excellent
    Grade.GOOD -> R.string.grade_body_good
    Grade.FAIR -> R.string.grade_body_fair
    Grade.POOR -> R.string.grade_body_poor
}

private fun UseCase.icon(): ImageVector = when (this) {
    UseCase.BROWSING -> Icons.Rounded.Language
    UseCase.STREAMING_HD -> Icons.Rounded.Hd
    UseCase.STREAMING_4K -> Icons.Rounded.LiveTv
    UseCase.VIDEO_CALLS -> Icons.Rounded.VideoCall
    UseCase.GAMING -> Icons.Rounded.SportsEsports
    UseCase.CLOUD_BACKUP -> Icons.Rounded.Backup
}

private fun UseCase.label() = when (this) {
    UseCase.BROWSING -> R.string.usecase_browsing
    UseCase.STREAMING_HD -> R.string.usecase_hd
    UseCase.STREAMING_4K -> R.string.usecase_4k
    UseCase.VIDEO_CALLS -> R.string.usecase_calls
    UseCase.GAMING -> R.string.usecase_gaming
    UseCase.CLOUD_BACKUP -> R.string.usecase_backup
}

private fun BloatGrade.text() = when (this) {
    BloatGrade.A_PLUS -> "A+"
    else -> name
}

@Composable
private fun BloatGrade.color(): Color = when (this) {
    BloatGrade.A_PLUS, BloatGrade.A -> Flare.palette.good
    BloatGrade.B, BloatGrade.C -> Flare.palette.warn
    BloatGrade.D, BloatGrade.F -> Flare.palette.bad
}

/** Provider and serving data centre as one line, e.g. "Deutsche Telekom AG · Server Düsseldorf". */
@Composable
fun providerLine(m: Measurement): String? {
    val info = m.info ?: return null
    val server = info.serverCity?.let { city -> info.serverCode?.let { "$city ($it)" } ?: city } ?: info.serverCode
    return when {
        info.isp != null && server != null -> stringResource(R.string.provider_server, info.isp, server)
        info.isp != null -> info.isp
        server != null -> stringResource(R.string.server_only, server)
        else -> null
    }
}

/** Everything a finished test means, in plain language: score, use cases, responsiveness, facts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultSummary(measurement: Measurement, networkType: NetworkType?, modifier: Modifier = Modifier) {
    val score = remember(measurement) { Quality.score(measurement) }
    val grade = Quality.grade(score)
    val colors = MaterialTheme.colorScheme

    FlareCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScoreRing(score, grade.color())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(grade.title()), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(grade.body()), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                providerLine(measurement)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        SectionLabel(stringResource(R.string.usecases_title))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UseCase.entries.forEach { useCase -> UseCaseChip(useCase, Quality.isSuitable(useCase, measurement)) }
        }

        Quality.latencyIncrease(measurement)?.let { increase ->
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
            BloatRow(Quality.bloatGrade(increase), increase)
        }

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        val facts = buildList {
            if (measurement.dataBytes > 0) add(stringResource(R.string.fact_data) to formatBytes(measurement.dataBytes))
            networkType?.let { add(stringResource(R.string.fact_network) to it.label()) }
            measurement.downloadLoadedMs?.let { add(stringResource(R.string.fact_loaded_down) to "${formatMillis(it)} ${stringResource(R.string.unit_ms)}") }
            measurement.uploadLoadedMs?.let { add(stringResource(R.string.fact_loaded_up) to "${formatMillis(it)} ${stringResource(R.string.unit_ms)}") }
        }
        facts.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        Text(stringResource(R.string.insight_note), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(alpha = 0.8f))
    }
}

@Composable
private fun ScoreRing(score: Int, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(score) { sweep.animateTo(score / 100f, tween(900)) }
    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, -90f, 360f, false, style = stroke)
            drawArc(color, -90f, 360f * sweep.value, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.headlineSmall.merge(NumberStyle), color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.score_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UseCaseChip(useCase: UseCase, ok: Boolean) {
    val palette = Flare.palette
    val colors = MaterialTheme.colorScheme
    val tint = if (ok) palette.good else colors.onSurfaceVariant.copy(alpha = 0.6f)
    Surface(
        shape = CircleShape,
        color = if (ok) palette.good.copy(alpha = 0.12f) else colors.surfaceContainerHigh,
        contentColor = if (ok) colors.onSurface else colors.onSurfaceVariant,
    ) {
        Row(Modifier.padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(useCase.icon(), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(stringResource(useCase.label()), style = MaterialTheme.typography.labelMedium)
            Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun BloatRow(grade: BloatGrade, increaseMs: Double) {
    val color = grade.color()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.16f), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(grade.text(), color = color, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.bloat_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.bloat_increase, formatMillis(increaseMs)), style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                stringResource(
                    when (grade) {
                        BloatGrade.A_PLUS, BloatGrade.A -> R.string.bloat_body_good
                        BloatGrade.B, BloatGrade.C -> R.string.bloat_body_ok
                        else -> R.string.bloat_body_bad
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
