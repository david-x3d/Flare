package dev.flare.speedtest.ui

import android.graphics.Bitmap
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.ui.navigation.FlareAppRoot
import dev.flare.speedtest.ui.theme.FlareTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Boots the real navigation shell with the real ViewModels and walks through the three tabs. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AppShellSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File("build/screenshots/$name.png").apply { parentFile!!.mkdirs() }
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show(size: DpSize) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlareTheme(dynamicColor = false) { FlareAppRoot(WindowSizeClass.calculateFromSize(size)) }
        }
        compose.mainClock.advanceTimeBy(1_500)
    }

    @Test
    fun phoneNavigatesThroughAllTabs() {
        show(DpSize(411.dp, 891.dp))
        compose.onNodeWithText("Start · Full").assertIsDisplayed()
        capture("shell_phone_test")

        compose.onNodeWithContentDescription("History").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("No tests yet").assertIsDisplayed()

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Unit").assertIsDisplayed()
        capture("shell_phone_settings")
    }

    @Config(sdk = [35], qualifiers = "w1100dp-h760dp-mdpi")
    @Test
    fun tabletUsesRailAndNavigates() {
        show(DpSize(1100.dp, 760.dp))
        compose.onNodeWithText("History").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("No tests yet").assertIsDisplayed()
        capture("shell_tablet_history")
    }
}
