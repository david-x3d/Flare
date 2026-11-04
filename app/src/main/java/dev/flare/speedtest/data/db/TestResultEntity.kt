package dev.flare.speedtest.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.flare.speedtest.domain.ConnectionInfo
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
    @ColumnInfo(name = "loaded_down_ms") val loadedDownMs: Double? = null,
    @ColumnInfo(name = "loaded_up_ms") val loadedUpMs: Double? = null,
    @ColumnInfo(name = "data_bytes", defaultValue = "0") val dataBytes: Long = 0,
    // The public IP is deliberately not stored.
    @ColumnInfo(name = "isp") val isp: String? = null,
    @ColumnInfo(name = "server_code") val serverCode: String? = null,
    @ColumnInfo(name = "server_city") val serverCity: String? = null,
)

fun TestResultEntity.toRecord() = SpeedTestRecord(
    id = id,
    timestampMillis = timestampMillis,
    networkType = runCatching { NetworkType.valueOf(networkType) }.getOrDefault(NetworkType.OTHER),
    measurement = Measurement(
        pingMs, jitterMs, downloadMbps, uploadMbps, decodeSamples(samplesJson),
        downloadLoadedMs = loadedDownMs, uploadLoadedMs = loadedUpMs, dataBytes = dataBytes,
        info = if (isp == null && serverCode == null) null else ConnectionInfo(isp = isp, serverCode = serverCode, serverCity = serverCity),
    ),
)

fun newEntity(networkType: NetworkType, measurement: Measurement, timestampMillis: Long) = TestResultEntity(
    timestampMillis = timestampMillis,
    networkType = networkType.name,
    pingMs = measurement.pingMs,
    jitterMs = measurement.jitterMs,
    downloadMbps = measurement.downloadMbps,
    uploadMbps = measurement.uploadMbps,
    samplesJson = encodeSamples(measurement.samples),
    loadedDownMs = measurement.downloadLoadedMs,
    loadedUpMs = measurement.uploadLoadedMs,
    dataBytes = measurement.dataBytes,
    isp = measurement.info?.isp,
    serverCode = measurement.info?.serverCode,
    serverCity = measurement.info?.serverCity,
)
