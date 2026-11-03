package dev.flare.speedtest.data.db

import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TransferSample
import org.json.JSONArray
import org.json.JSONObject

internal fun encodeSamples(samples: List<TransferSample>): String = JSONArray().apply {
    samples.forEach { s -> put(JSONObject().apply {
        put("step", s.stepIndex); put("round", s.round); put("phase", s.phase.name)
        put("bytes", s.bytes); put("time", s.timestampMillis); put("status", s.status)
        put("mbps", s.mbps); put("duration", s.durationMs); put("latency", s.latencyMs)
        put("server", s.serverTimeMs); put("response", s.responseBytes)
        put("loaded", JSONArray(s.loadedLatencies))
    }) }
}.toString()

internal fun decodeSamples(json: String): List<TransferSample> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { index ->
        val o = array.getJSONObject(index)
        fun number(key: String): Double? = if (o.isNull(key)) null else o.getDouble(key)
        val loaded = o.getJSONArray("loaded")
        TransferSample(o.getInt("step"), o.getInt("round"), TestPhase.valueOf(o.getString("phase")),
            o.getLong("bytes"), o.getLong("time"), number("mbps"), number("duration"), number("latency"),
            number("server"), if (o.isNull("response")) null else o.getLong("response"),
            (0 until loaded.length()).map { loaded.getDouble(it) }, o.getString("status"))
    }
}.getOrDefault(emptyList())
