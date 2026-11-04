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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.ui.history.RecordSheet
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

/** Phones get a floating pill bar, wider windows (tablets, foldables) a navigation rail. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun FlareAppRoot(
    windowSizeClass: WindowSizeClass,
    launchAction: String? = null,
    onLaunchActionHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.entries.firstOrNull { it.route == backStackEntry?.destination?.route } ?: Destination.TEST

    val compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val twoPane = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    // Room for the floating bar so the last list item can scroll above it.
    val bottomPadding = if (compact) 104.dp else 24.dp

    // Activity-scoped, so a running test survives switching tabs.
    val testViewModel: TestViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val testState by testViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var openRecord by remember { mutableStateOf<SpeedTestRecord?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    fun delete(record: SpeedTestRecord) {
        historyViewModel.delete(record.id)
        openRecord = null
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) historyViewModel.restore(record)
        }
    }

    LaunchedEffect(launchAction) {
        when (launchAction) {
            ACTION_START_TEST -> {
                navController.navigateTopLevel(Destination.TEST)
                testViewModel.start()
            }
            ACTION_HISTORY -> navController.navigateTopLevel(Destination.HISTORY)
            else -> return@LaunchedEffect
        }
        onLaunchActionHandled()
    }

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
                        onMode = testViewModel::selectMode,
                        lastRecord = historyState.records.firstOrNull(),
                        onOpenRecord = { openRecord = it },
                    )
                }
                composable(Destination.HISTORY.route) {
                    HistoryScreen(
                        state = historyState,
                        onDelete = ::delete,
                        onClear = historyViewModel::clear,
                        twoPane = twoPane,
                        bottomPadding = bottomPadding,
                        onOpen = { openRecord = it },
                        onRunTest = { navController.navigateTopLevel(Destination.TEST) },
                    )
                }
                composable(Destination.SETTINGS.route) {
                    SettingsScreen(
                        settings = settings,
                        onUnit = settingsViewModel::setUnit,
                        onDefaultMode = settingsViewModel::setDefaultMode,
                        onKeepScreenOn = settingsViewModel::setKeepScreenOn,
                        onSaveHistory = settingsViewModel::setSaveHistory,
                        onTestSize = settingsViewModel::setTestSize,
                        onTheme = settingsViewModel::setThemeMode,
                        onDynamicColor = settingsViewModel::setDynamicColor,
                        onHaptics = settingsViewModel::setHaptics,
                        bottomPadding = bottomPadding,
                    )
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SnackbarHost(snackbarHost, modifier = Modifier.padding(horizontal = 12.dp))
                if (compact) {
                    FlareFloatingBar(current, onSelect = navController::navigateTopLevel)
                }
            }
        }
    }

    openRecord?.let { record ->
        RecordSheet(
            record = record,
            unit = historyState.unit,
            onDismiss = { openRecord = null },
            onDelete = ::delete,
        )
    }
}

const val ACTION_START_TEST = "dev.flare.speedtest.action.START_TEST"
const val ACTION_HISTORY = "dev.flare.speedtest.action.HISTORY"

private fun NavHostController.navigateTopLevel(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FlareFloatingBar(current: Destination, onSelect: (Destination) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 8.dp, bottom = 12.dp)
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.forEach { destination ->
                val selected = destination == current
                val label = stringResource(destination.label)
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(role = Role.Tab) { if (!selected) onSelect(destination) }
                        .padding(horizontal = if (selected) 20.dp else 18.dp)
                        .semantics { if (!selected) contentDescription = label },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    AnimatedVisibility(
                        visible = selected,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut(),
                    ) {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp),
                            maxLines = 1,
                        )
                    }
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
                androidx.compose.ui.res.painterResource(R.drawable.flare_logo),
                contentDescription = stringResource(R.string.app_name),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
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
