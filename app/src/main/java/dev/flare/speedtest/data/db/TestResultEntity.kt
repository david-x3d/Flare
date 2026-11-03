package dev.flare.speedtest.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SpeedTestRecord

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestampMillis: Long,
    @ColumnInfo(name = "network_type") val networkType: String,
    @ColumnInfo(name = "ping_ms") val pingMs: Double,
    @ColumnInfo(name = "jitter_ms") val jitterMs: Double,
    @ColumnInfo(name = "download_mbps") val downloadMbps: Double,
    @ColumnInfo(name = "upload_mbps") val uploadMbps: Double,
    @ColumnInfo(name = "samples_json", defaultValue = "'[]'") val samplesJson: String = "[]",
)

fun TestResultEntity.toRecord() = SpeedTestRecord(
    id = id,
    timestampMillis = timestampMillis,
    networkType = runCatching { NetworkType.valueOf(networkType) }.getOrDefault(NetworkType.OTHER),
    measurement = Measurement(pingMs, jitterMs, downloadMbps, uploadMbps, decodeSamples(samplesJson)),
)

fun newEntity(networkType: NetworkType, measurement: Measurement, timestampMillis: Long) = TestResultEntity(
    timestampMillis = timestampMillis,
    networkType = networkType.name,
    pingMs = measurement.pingMs,
    jitterMs = measurement.jitterMs,
    downloadMbps = measurement.downloadMbps,
    uploadMbps = measurement.uploadMbps,
    samplesJson = encodeSamples(measurement.samples),
)
