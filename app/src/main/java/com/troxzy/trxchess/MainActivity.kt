package com.troxzy.trxchess

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.screens.AboutScreen
import com.troxzy.trxchess.ui.screens.AnalysisScreen
import com.troxzy.trxchess.ui.screens.DiagnosticsScreen
import com.troxzy.trxchess.ui.screens.EditorScreen
import com.troxzy.trxchess.ui.screens.HistoryScreen
import com.troxzy.trxchess.ui.screens.HomeDestination
import com.troxzy.trxchess.ui.screens.HomeScreen
import com.troxzy.trxchess.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Single-activity host with a custom back stack. Screens are plain views
 * swapped in a container; ViewModels are activity-scoped so state survives
 * navigation. Theme changes recreate the current screen view (state kept in
 * the ViewModels).
 */
class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private val backStack = ArrayDeque<Screen>()
    private lateinit var screenHost: FrameLayout

    private val overlayPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val granted = Settings.canDrawOverlays(this)
            container.appScope.launch {
                container.settings.update { it.copy(overlayEnabled = granted) }
            }
            if (granted) container.setOverlayRunning(true)
        }

    private val backHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (backStack.size > 1) {
                backStack.removeLast()
                showScreen(backStack.last())
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as TrxApp).container
        WindowCompat.setDecorFitsSystemWindows(window, false)

        screenHost = FrameLayout(this)
        setContentView(screenHost)
        onBackPressedDispatcher.addCallback(this, backHandler)

        container.startSignalMonitoring()
        container.designSystem.themeMode = container.settings.settings.value.themeMode
        container.designSystem.observeColors { recreateCurrentScreen() }

        backStack.addLast(Screen.Home)
        showScreen(Screen.Home)
    }

    override fun onResume() {
        super.onResume()
        (currentRoot() as? DiagnosticsScreen)?.refresh()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        container.designSystem.onUiModeChanged(newConfig.uiMode)
    }

    private fun currentRoot(): View? = screenHost.getChildAt(screenHost.childCount - 1)

    private fun recreateCurrentScreen() {
        if (backStack.isEmpty()) return
        showScreen(backStack.last())
    }

    private fun showScreen(screen: Screen) {
        screenHost.removeAllViews()
        val view: View = when (screen) {
            Screen.Home -> HomeScreen(this, container, ::navigate)
            is Screen.Analysis -> AnalysisScreen(this, container, screen.fen)
            Screen.Editor -> EditorScreen(this, container, onAnalyze = { fen ->
                navigate(Screen.Analysis(fen))
            })
            Screen.History -> HistoryScreen(this, container, onOpenSession = { session ->
                navigate(Screen.Analysis(session.initialFen))
            })
            Screen.Settings -> SettingsScreen(this, container, ::requestOverlayPermission)
            Screen.Diagnostics -> DiagnosticsScreen(this, container)
            Screen.About -> AboutScreen(this, container)
        }
        screenHost.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun navigate(screen: Screen) {
        backStack.addLast(screen)
        showScreen(screen)
    }

    private fun navigate(destination: HomeDestination) {
        val screen = when (destination) {
            HomeDestination.QUICK_ANALYSIS -> Screen.Analysis(null)
            HomeDestination.ADVANCED_ANALYSIS -> Screen.Analysis(null)
            HomeDestination.BOARD_EDITOR -> Screen.Editor
            HomeDestination.HISTORY -> Screen.History
            HomeDestination.OVERLAY -> Screen.Settings
            HomeDestination.SETTINGS -> Screen.Settings
            HomeDestination.DIAGNOSTICS -> Screen.Diagnostics
            HomeDestination.ABOUT -> Screen.About
        }
        navigate(screen)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            runCatching {
                overlayPermission.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }
        }
    }

    sealed interface Screen {
        data object Home : Screen
        data class Analysis(val fen: String?) : Screen
        data object Editor : Screen
        data object History : Screen
        data object Settings : Screen
        data object Diagnostics : Screen
        data object About : Screen
    }
}