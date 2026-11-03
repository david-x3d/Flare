package dev.flare.speedtest.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import dev.flare.speedtest.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryMigrationTest {
    @Test fun `version one history survives migration and new transfer details round trip`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("flare.db")
        val file = context.getDatabasePath("flare.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE test_results (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, network_type TEXT NOT NULL, ping_ms REAL NOT NULL, jitter_ms REAL NOT NULL, download_mbps REAL NOT NULL, upload_mbps REAL NOT NULL)")
            db.execSQL("INSERT INTO test_results VALUES (1, 42, 'WIFI', 12.0, 1.0, 100.0, 20.0)")
            db.version = 1
        }
        val database = FlareDatabase.create(context)
        try {
            val old = database.testResultDao().observeRecent(10).first().single().toRecord()
            assertEquals(100.0, old.measurement.downloadMbps, 0.0)
            assertTrue(old.measurement.samples.isEmpty())
            val sample = TransferSample(0, 1, TestPhase.DOWNLOAD, 100_000, 99,
                mbps = 30.0, durationMs = 50.0, responseBytes = 100_000, loadedLatencies = listOf(12.0, 15.0))
            val measurement = Measurement(12.0, 1.0, 30.0, 20.0, listOf(sample))
            database.testResultDao().insert(newEntity(NetworkType.WIFI, measurement, 99))
            val saved = database.testResultDao().observeRecent(10).first().first().toRecord()
            assertEquals(measurement, saved.measurement)
        } finally {
            database.close()
            context.deleteDatabase("flare.db")
        }
    }
}
