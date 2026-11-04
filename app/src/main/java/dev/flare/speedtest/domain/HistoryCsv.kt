package dev.flare.speedtest.domain

import java.time.Instant

/** Locale-independent export; units in headers stay stable across display preferences. */
fun historyCsv(records: List<SpeedTestRecord>): String = buildString {
    append("timestamp,network,download_mbps,upload_mbps,ping_ms,jitter_ms,loaded_down_ms,loaded_up_ms,data_bytes,isp,server\r\n")
    records.forEach { record ->
        val m = record.measurement
        append(listOf(Instant.ofEpochMilli(record.timestampMillis), record.networkType.name,
            m.downloadMbps, m.uploadMbps, m.pingMs, m.jitterMs, m.downloadLoadedMs ?: "", m.uploadLoadedMs ?: "",
            m.dataBytes, csvField(m.info?.isp), csvField(m.info?.serverCode)).joinToString(","))
        append("\r\n")
    }
}

/** Raw transfers use the reference CSV columns; additional fields identify skipped/failed rounds. */
fun transferCsv(samples: List<TransferSample>): String = buildString {
    append("time,direction,bytes,latency,bps,duration,serverTime,responseSize,loadedLatencies,step,round,status\r\n")
    samples.forEach { s ->
        append(listOf(s.timestampMillis, s.phase.name.lowercase(java.util.Locale.ROOT), s.bytes,
            s.latencyMs ?: "", s.mbps?.times(1_000_000) ?: "", s.durationMs ?: "", s.serverTimeMs ?: "",
            s.responseBytes ?: "", s.loadedLatencies.joinToString(" "), s.stepIndex + 1, s.round, s.status).joinToString(","))
        append("\r\n")
    }
}

private fun csvField(value: String?): String {
    if (value == null) return ""
    return if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
