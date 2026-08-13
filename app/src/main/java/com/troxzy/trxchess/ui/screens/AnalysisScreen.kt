package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.troxzy.trxchess.R
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.chess.PieceType
import com.troxzy.trxchess.chess.PositionStatus
import com.troxzy.trxchess.chess.Side
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.analysis.EvalBarView
import com.troxzy.trxchess.ui.analysis.EvalGraphView
import com.troxzy.trxchess.ui.analysis.EngineStatusView
import com.troxzy.trxchess.ui.board.BoardThemes
import com.troxzy.trxchess.ui.board.BoardView
import com.troxzy.trxchess.ui.common.PromotionDialogView
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
 * graph, stats, PV line, FEN import, promotion dialog and controls. Renders
 * [AnalysisUiState] only — all logic lives in [AnalysisViewModel].
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
    private val statusText = TextView(context).apply {
        textSize = TypeTokens.BodyStrong.sizeSp
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = Gravity.CENTER
        visibility = View.GONE
    }
    private val engineErrorText = TextView(context).apply {
        textSize = TypeTokens.Caption.sizeSp
        setTextColor(designSystem.colors.danger)
        gravity = Gravity.CENTER
        visibility = View.GONE
        maxLines = 2
    }
    private val fenErrorText = TextView(context).apply {
        textSize = TypeTokens.Caption.sizeSp
        setTextColor(designSystem.colors.danger)
        maxLines = 2
        visibility = View.GONE
    }

    private val fenInput = EditText(context).apply {
        hint = resources.getString(R.string.analysis_fen_hint)
        setTextColor(designSystem.colors.textPrimary)
        setHintTextColor(designSystem.colors.textMuted)
        textSize = TypeTokens.Caption.sizeSp
        typeface = Typeface.create("monospace", Typeface.NORMAL)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_ACTION_DONE
        maxLines = 2
        setSingleLine(false)
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.loadFen(text.toString())
                true
            } else false
        }
        setText(if (initialFen != null) initialFen else "")
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
    private val fenLoadButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_fen_load)
        style = ButtonStyle.SECONDARY
        setOnClickListener { viewModel.loadFen(fenInput.text.toString()) }
    }
    private val fenClearButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.analysis_fen_clear)
        style = ButtonStyle.GHOST
        setOnClickListener { fenInput.setText("") }
    }
    private val retryButton = TrxButton(context, null, designSystem).apply {
        text = resources.getString(R.string.error_retry)
        style = ButtonStyle.SECONDARY
        setOnClickListener { viewModel.retryEngine() }
    }

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.analysis_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    private val promotionDialog = PromotionDialogView(
        context,
        designSystem,
        onPick = { type: PieceType -> viewModel.choosePromotion(type) },
        onCancel = { viewModel.cancelPromotion() },
    )

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })

        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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

        content.addView(statusText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        // FEN import row
        val fenRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(2), dp(16), dp(2))
        }
        fenRow.addView(
            fenInput,
            LinearLayout.LayoutParams(0, dp(48), 1f),
        )
        fenRow.addView(fenLoadButton, LinearLayout.LayoutParams(dp(76), dp(44)).apply { leftMargin = dp(8) })
        fenRow.addView(fenClearButton, LinearLayout.LayoutParams(dp(64), dp(44)).apply { leftMargin = dp(4) })
        content.addView(fenRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        content.addView(
            fenErrorText,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(20)
                rightMargin = dp(20)
            },
        )

        // engine error banner
        val errorRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        errorRow.addView(engineErrorText, LinearLayout.LayoutParams(0, dp(36), 1f))
        errorRow.addView(retryButton, LinearLayout.LayoutParams(dp(84), dp(40)).apply { leftMargin = dp(8) })
        content.addView(errorRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))

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
        promotionDialog.dismiss()
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

        renderStatus(state.status)
        renderFenErrors(state.fenError)
        renderEngineError(state.engineError)

        analyzeButton.enabledState = !state.thinking && state.engineState !is com.troxzy.trxchess.engine.api.EngineState.Failed
        stopButton.enabledState = state.thinking
        undoButton.enabledState = state.history.isNotEmpty()
        newButton.enabledState = state.history.isNotEmpty() || state.analysis != null

        if (state.pendingPromotion != null) {
            promotionDialog.show(state.position.sideToMove)
        } else {
            promotionDialog.dismiss()
        }
    }

    private fun renderStatus(status: PositionStatus) {
        when (status) {
            PositionStatus.NORMAL -> statusText.visibility = View.GONE
            PositionStatus.CHECK -> {
                statusText.visibility = View.VISIBLE
                statusText.setText(R.string.position_status_check)
                statusText.setTextColor(designSystem.colors.warning)
            }
            PositionStatus.CHECKMATE -> {
                statusText.visibility = View.VISIBLE
                statusText.setText(R.string.position_status_checkmate)
                statusText.setTextColor(designSystem.colors.danger)
            }
            PositionStatus.STALEMATE -> {
                statusText.visibility = View.VISIBLE
                statusText.setText(R.string.position_status_stalemate)
                statusText.setTextColor(designSystem.colors.textSecondary)
            }
        }
    }

    private fun renderFenErrors(error: Fen.FenError?) {
        if (error == null) {
            fenErrorText.visibility = View.GONE
            return
        }
        val id = when (error) {
            Fen.FenError.INVALID_FIELD_COUNT -> R.string.fen_error_field_count
            Fen.FenError.INVALID_BOARD -> R.string.fen_error_board
            Fen.FenError.INVALID_PIECE -> R.string.fen_error_piece
            Fen.FenError.INVALID_SIDE_TO_MOVE -> R.string.fen_error_side_to_move
            Fen.FenError.INVALID_CASTLING_RIGHTS -> R.string.fen_error_castling
            Fen.FenError.INVALID_EN_PASSANT -> R.string.fen_error_en_passant
            Fen.FenError.INVALID_HALFMOVE_CLOCK -> R.string.fen_error_halfmove
            Fen.FenError.INVALID_FULLMOVE_NUMBER -> R.string.fen_error_fullmove
        }
        fenErrorText.setText(id)
        fenErrorText.visibility = View.VISIBLE
    }

    private fun renderEngineError(error: String?) {
        if (error == null) {
            engineErrorText.visibility = View.GONE
            retryButton.visibility = View.GONE
            return
        }
        engineErrorText.text = error
        engineErrorText.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
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