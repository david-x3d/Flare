package dev.flare.speedtest.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.flare.speedtest.R
import dev.flare.speedtest.ui.AppViewModelProvider
import dev.flare.speedtest.ui.history.HistoryScreen
import dev.flare.speedtest.ui.history.HistoryViewModel
import dev.flare.speedtest.ui.settings.SettingsScreen
import dev.flare.speedtest.ui.settings.SettingsViewModel
import dev.flare.speedtest.ui.test.TestScreen
import dev.flare.speedtest.ui.test.TestViewModel

enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    TEST("test", R.string.nav_test, Icons.Rounded.Speed),
    HISTORY("history", R.string.nav_history, Icons.Rounded.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Settings),
}

/** Phones get a floating toolbar, wider windows (tablets, foldables) a navigation rail. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun FlareAppRoot(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.entries.firstOrNull { it.route == backStackEntry?.destination?.route } ?: Destination.TEST

    val compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val twoPane = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    // Room for the floating toolbar so the last list item can scroll above it.
    val bottomPadding = if (compact) 104.dp else 24.dp

    // Activity-scoped, so a running test survives switching tabs.
    val testViewModel: TestViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val testState by testViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<Float>()
    val effects = motion.defaultEffectsSpec<Float>()

    Row(modifier = Modifier.fillMaxSize()) {
        if (!compact) {
            FlareRail(current, onSelect = navController::navigateTopLevel)
        }
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Destination.TEST.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(effects) + scaleIn(initialScale = 0.94f, animationSpec = spatial) },
                exitTransition = { fadeOut(effects) },
                popEnterTransition = { fadeIn(effects) + scaleIn(initialScale = 0.94f, animationSpec = spatial) },
                popExitTransition = { fadeOut(effects) + scaleOut(targetScale = 0.94f, animationSpec = spatial) },
            ) {
                composable(Destination.TEST.route) {
                    TestScreen(
                        state = testState,
                        onStart = testViewModel::start,
                        onStop = testViewModel::stop,
                        onDismissError = testViewModel::dismissError,
                        onTestSizeSelected = settingsViewModel::setTestSize,
                        twoPane = twoPane,
                        bottomPadding = bottomPadding,
                    )
                }
                composable(Destination.HISTORY.route) {
                    HistoryScreen(
                        state = historyState,
                        onDelete = historyViewModel::delete,
                        onClear = historyViewModel::clear,
                        twoPane = twoPane,
                        bottomPadding = bottomPadding,
                    )
                }
                composable(Destination.SETTINGS.route) {
                    SettingsScreen(
                        settings = settings,
                        onUnit = settingsViewModel::setUnit,
                        onTestSize = settingsViewModel::setTestSize,
                        onTheme = settingsViewModel::setThemeMode,
                        onDynamicColor = settingsViewModel::setDynamicColor,
                        bottomPadding = bottomPadding,
                    )
                }
            }
            if (compact) {
                FlareFloatingToolbar(
                    current = current,
                    onSelect = navController::navigateTopLevel,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FlareFloatingToolbar(current: Destination, onSelect: (Destination) -> Unit, modifier: Modifier = Modifier) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 16.dp),
    ) {
        Destination.entries.forEach { destination ->
            val selected = destination == current
            ToggleButton(
                checked = selected,
                onCheckedChange = { if (!selected) onSelect(destination) },
            ) {
                Icon(destination.icon, contentDescription = if (selected) null else stringResource(destination.label), modifier = Modifier.size(22.dp))
                AnimatedVisibility(
                    visible = selected,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Text(stringResource(destination.label), modifier = Modifier.padding(start = 8.dp), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FlareRail(current: Destination, onSelect: (Destination) -> Unit) {
    NavigationRail(
        header = {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = stringResource(R.string.app_name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
    ) {
        Destination.entries.forEach { destination ->
            NavigationRailItem(
                selected = destination == current,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
