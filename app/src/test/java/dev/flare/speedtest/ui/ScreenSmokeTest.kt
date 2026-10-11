package dev.flare.speedtest.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.ThemeMode
import dev.flare.speedtest.ui.history.HistoryScreen
import dev.flare.speedtest.ui.history.HistoryUiState
import dev.flare.speedtest.ui.settings.SettingsScreen
import dev.flare.speedtest.ui.test.TestScreen
import dev.flare.speedtest.ui.test.TestUiState
import dev.flare.speedtest.ui.theme.FlareTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screens headlessly. Guards against composition crashes and, as a side effect,
 * writes screenshots to `build/screenshots` so the layout can be eyeballed without a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val measurement = Measurement(pingMs = 14.2, jitterMs = 2.1, downloadMbps = 487.3, uploadMbps = 96.4)

    private fun records() = (1..12).map {
        SpeedTestRecord(
            id = it.toLong(),
            timestampMillis = 1_760_000_000_000L + it * 3_600_000L,
            networkType = if (it % 3 == 0) NetworkType.CELLULAR else NetworkType.WIFI,
            measurement = measurement.copy(downloadMbps = 200.0 + it * 23 % 300, uploadMbps = 40.0 + it * 7 % 80),
        )
    }

    private fun render(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlareTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) { Box { content() } }
            }
        }
        // Let the springs settle; infinite animations keep the clock busy, so advance by hand.
        compose.mainClock.advanceTimeBy(2_500)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File("build/screenshots/$name.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 1_000)
    }

    private fun testScreen(state: TestUiState, twoPane: Boolean = false) = @Composable {
        TestScreen(
            state = state, onStart = {}, onStop = {}, onDismissError = {}, onTestSizeSelected = {},
            twoPane = twoPane, bottomPadding = 104.dp,
        )
    }

    @Test fun testIdle() = render("test_idle", content = testScreen(TestUiState(networkType = NetworkType.WIFI)))

    @Test fun testIdleDark() = render("test_idle_dark", dark = true, content = testScreen(TestUiState(networkType = NetworkType.CELLULAR)))

    @Test fun testPing() = render(
        "test_ping",
        content = testScreen(TestUiState(phase = TestPhase.PING, progress = 0.06f, livePingMs = 18.4, networkType = NetworkType.WIFI)),
    )

    @Test fun testDownload() = render(
        "test_download",
        content = testScreen(
            TestUiState(
                phase = TestPhase.DOWNLOAD, progress = 0.3f, liveMbps = 312.5, pingMs = 14.2, jitterMs = 2.1,
                livePingMs = 14.2, networkType = NetworkType.WIFI,
            ),
        ),
    )

    @Test fun testUpload() = render(
        "test_upload",
        content = testScreen(
            TestUiState(
                phase = TestPhase.UPLOAD, progress = 0.75f, liveMbps = 88.1, pingMs = 14.2, jitterMs = 2.1,
                downloadMbps = 487.3, networkType = NetworkType.WIFI,
            ),
        ),
    )

    @Test fun testDone() = render(
        "test_done",
        content = testScreen(
            TestUiState(
                phase = TestPhase.DONE, progress = 1f, pingMs = measurement.pingMs, jitterMs = measurement.jitterMs,
                downloadMbps = measurement.downloadMbps, uploadMbps = measurement.uploadMbps, networkType = NetworkType.WIFI,
            ),
        ),
    )

    @Test fun testError() = render(
        "test_error",
        content = testScreen(TestUiState(error = SpeedTestError.EndpointChanged("https://speed.cloudflare.com/__down", 404, null))),
    )

    @Config(sdk = [35], qualifiers = "w1100dp-h760dp-mdpi")
    @Test fun testDoneTwoPane() = render(
        "test_done_two_pane",
        content = testScreen(
            TestUiState(
                phase = TestPhase.DONE, progress = 1f, pingMs = measurement.pingMs, jitterMs = measurement.jitterMs,
                downloadMbps = measurement.downloadMbps, uploadMbps = measurement.uploadMbps,
            ),
            twoPane = true,
        ),
    )

    @Test fun history() = render("history", content = {
        HistoryScreen(HistoryUiState(loading = false, records = records().reversed()), onDelete = {}, onClear = {}, twoPane = false, bottomPadding = 104.dp)
    })

    @Test fun historyEmpty() = render("history_empty", content = {
        HistoryScreen(HistoryUiState(loading = false), onDelete = {}, onClear = {}, twoPane = false, bottomPadding = 104.dp)
    })

    @Test fun settings() = render("settings", dark = true, content = {
        SettingsScreen(AppSettings(), {}, {}, {}, {}, bottomPadding = 104.dp)
    })
}
