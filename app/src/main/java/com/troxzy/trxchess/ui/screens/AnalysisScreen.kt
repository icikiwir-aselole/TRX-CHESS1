package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.troxzy.trxchess.R
import com.troxzy.trxchess.chess.Move
import com.troxzy.trxchess.chess.Side
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.analysis.EvalBarView
import com.troxzy.trxchess.ui.analysis.EvalGraphView
import com.troxzy.trxchess.ui.analysis.EngineStatusView
import com.troxzy.trxchess.ui.board.BoardThemes
import com.troxzy.trxchess.ui.board.BoardView
import com.troxzy.trxchess.ui.common.TopBarView
import com.troxzy.trxchess.ui.components.ButtonStyle
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.components.TrxButton
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens
import com.troxzy.trxchess.vm.AnalysisUiState
import com.troxzy.trxchess.vm.AnalysisViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Analysis screen: eval bar, animated board, engine status, evaluation
 * graph, stats, PV line and controls. Renders [AnalysisUiState] only — all
 * logic lives in [AnalysisViewModel].
 */
class AnalysisScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    initialFen: String? = null,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val activity = context as ComponentActivity
    private val viewModel: AnalysisViewModel = ViewModelProvider(
        activity,
        AnalysisViewModel.factory(container),
    )[AnalysisViewModel::class.java]

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val dpF = resources.displayMetrics.density

    private val board = BoardView(context, null, designSystem)
    private val evalBar = EvalBarView(context, null, designSystem)
    private val engineStatus = EngineStatusView(context, null, designSystem)
    private val evalGraph = EvalGraphView(context, null, designSystem)
    private val depthValue = statLabel()
    private val nodesValue = statLabel()
    private val npsValue = statLabel()
    private val pvLine = TextView(context).apply {
        textSize = TypeTokens.Caption.sizeSp
        typeface = Typeface.create("monospace", Typeface.NORMAL)
        setTextColor(designSystem.colors.textSecondary)
        maxLines = 2
        setText(R.string.status_unknown)
    }

    private val analyzeButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_controls_analyze)
        style = ButtonStyle.PRIMARY
        setOnClickListener { viewModel.startAnalysis() }
    }
    private val stopButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_controls_stop)
        style = ButtonStyle.SECONDARY
        setOnClickListener { viewModel.stopAnalysis() }
    }
    private val undoButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_controls_undo)
        style = ButtonStyle.GHOST
        setOnClickListener { viewModel.undo() }
    }
    private val newButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_controls_new)
        style = ButtonStyle.GHOST
        setOnClickListener { viewModel.newGame() }
    }
    private val flipButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_flip)
        style = ButtonStyle.GHOST
        setOnClickListener { viewModel.flip() }
    }

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.analysis_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })

        // board row: eval bar + board
        val boardArea = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val boardSize = (resources.displayMetrics.widthPixels - dp(22)).coerceAtMost(dp(520))
        evalBar.layoutParams = LinearLayout.LayoutParams(dp(22), boardSize)
        boardArea.addView(evalBar)
        boardArea.addView(
            board,
            LinearLayout.LayoutParams(boardSize, boardSize),
        )
        content.addView(boardArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, boardSize))

        content.addView(engineStatus, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))

        content.addView(
            evalGraph,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply { leftMargin = dp(8); rightMargin = dp(8) },
        )

        // stats row
        val stats = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        stats.addView(statCell(resources.getString(R.string.analysis_depth), depthValue), LinearLayout.LayoutParams(0, dp(40), 1f))
        stats.addView(statCell(resources.getString(R.string.analysis_nodes), nodesValue), LinearLayout.LayoutParams(0, dp(40), 1f))
        stats.addView(statCell(resources.getString(R.string.analysis_nps), npsValue), LinearLayout.LayoutParams(0, dp(40), 1f))
        content.addView(stats, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)))

        content.addView(
            pvLine,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { leftMargin = dp(16); rightMargin = dp(16) },
        )

        // controls
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controls.addView(analyzeButton, LinearLayout.LayoutParams(dp(96), dp(44)))
        controls.addView(stopButton, LinearLayout.LayoutParams(dp(88), dp(44)).apply { leftMargin = dp(8) })
        controls.addView(undoButton, LinearLayout.LayoutParams(dp(80), dp(44)).apply { leftMargin = dp(8) })
        controls.addView(newButton, LinearLayout.LayoutParams(dp(88), dp(44)).apply { leftMargin = dp(8) })
        content.addView(controls, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(4) })

        content.addView(
            flipButton,
            LinearLayout.LayoutParams(dp(110), dp(40)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                leftMargin = dp(16)
                rightMargin = dp(16)
            },
        )

        board.interactive = true
        board.onSquareTap = { viewModel.onSquareTap(it) }
        board.onMovePlayed = { viewModel.onMovePlayed(it) }

        if (initialFen != null) viewModel.loadFen(initialFen)
        viewModel.bindOverlay()

        scope.launch {
            viewModel.state.collect { state -> render(state) }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    private fun render(state: AnalysisUiState) {
        board.setPosition(state.position)
        board.setSelected(state.selected)
        board.setLegalTargets(state.legalTargets)
        board.setLastMove(state.history.lastOrNull())
        board.flipped = state.flipped
        board.theme = BoardThemes.byId(container.settings.settings.value.boardThemeId)
        if (state.engineState is com.troxzy.trxchess.engine.api.EngineState.Failed) {
            board.setCheckSquare(null)
        } else {
            val king = state.position.board.entries.firstOrNull {
                it.value.side == state.position.sideToMove && it.value.type == com.troxzy.trxchess.chess.PieceType.KING
            }?.key
            board.setCheckSquare(king?.takeIf { state.position.inCheck(state.position.sideToMove) })
        }

        engineStatus.engineState = state.engineState
        engineStatus.depth = state.depth
        engineStatus.nodes = state.nodes
        engineStatus.nps = state.nps

        val best = state.analysis?.lines?.firstOrNull()
        evalBar.evaluation = best?.evaluation ?: com.troxzy.trxchess.engine.api.Evaluation.Centipawn(0)

        depthValue.text = state.depth.toString()
        nodesValue.text = state.nodes.toString()
        npsValue.text = state.nps.toString()

        val line = best?.pv?.joinToString(" ").orEmpty()
        pvLine.text = line.ifEmpty { resources.getString(R.string.analysis_best_line) + ": —" }

        analyzeButton.enabledState = !state.thinking && state.engineState !is com.troxzy.trxchess.engine.api.EngineState.Failed
        stopButton.enabledState = state.thinking
        undoButton.enabledState = state.history.isNotEmpty()
        newButton.enabledState = state.history.isNotEmpty() || state.analysis != null
    }

    private fun statCell(label: String, value: TextView): LinearLayout {
        val cell = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        cell.addView(TextView(context).apply {
            text = label
            setTextColor(designSystem.colors.textMuted)
            textSize = TypeTokens.Caption.sizeSp
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)))
        cell.addView(value, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)))
        return cell
    }

    private fun statLabel(): TextView = TextView(context).apply {
        setTextColor(designSystem.colors.textPrimary)
        textSize = TypeTokens.BodyStrong.sizeSp
        typeface = Typeface.create("monospace", Typeface.BOLD)
        gravity = Gravity.CENTER
        text = "—"
    }

    private fun dp(v: Int): Int = (v * dpF).toInt()
}