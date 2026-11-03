package dev.flare.speedtest.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryCsvTest {
    @Test fun `export uses ISO dates and decimal dots regardless of display locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val record = SpeedTestRecord(1, 0, NetworkType.WIFI, Measurement(12.5, 1.2, 250.25, 40.5))
            assertEquals("timestamp,network,download_mbps,upload_mbps,ping_ms,jitter_ms\r\n1970-01-01T00:00:00Z,WIFI,250.25,40.5,12.5,1.2\r\n", historyCsv(listOf(record)))
        } finally { Locale.setDefault(previous) }
    }
}
