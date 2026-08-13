package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.troxzy.trxchess.R
import com.troxzy.trxchess.chess.Piece
import com.troxzy.trxchess.chess.PieceType
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.board.BoardThemes
import com.troxzy.trxchess.ui.board.BoardView
import com.troxzy.trxchess.ui.common.TopBarView
import com.troxzy.trxchess.ui.components.ButtonStyle
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.components.TrxButton
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens
import com.troxzy.trxchess.vm.EditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Board editor: place/remove pieces by tapping, load FEN, start a fresh
 * position, then hand the resulting position to the analysis screen.
 */
class EditorScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    private val onAnalyze: (String) -> Unit,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val activity = context as ComponentActivity
    private val viewModel: EditorViewModel = ViewModelProvider(
        activity,
        EditorViewModel.factory(container),
    )[EditorViewModel::class.java]

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val dpF = resources.displayMetrics.density

    private val board = BoardView(context, null, designSystem)
    private val fenInput = EditText(context).apply {
        hint = resources.getString(R.string.editor_fen_hint)
        setTextColor(designSystem.colors.textPrimary)
        setHintTextColor(designSystem.colors.textMuted)
        textSize = 12f
        typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        isSingleLine = false
        maxLines = 2
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }
    private val fenError = TextView(context).apply {
        setTextColor(designSystem.colors.danger)
        textSize = TypeTokens.Caption.sizeSp
        visibility = View.GONE
    }

    private val piecePalette = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private var pieceButtons = mutableListOf<PieceButton>()
    private val sideWhite = TrxButton(context, null, designSystem).apply {
        text = "White"
        style = ButtonStyle.PRIMARY
        setOnClickListener { viewModel.setSide(true) }
    }
    private val sideBlack = TrxButton(context, null, designSystem).apply {
        text = "Black"
        style = ButtonStyle.SECONDARY
        setOnClickListener { viewModel.setSide(false) }
    }

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.editor_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })

        val boardSize = resources.displayMetrics.widthPixels - dp(16)
        content.addView(
            board,
            LinearLayout.LayoutParams(boardSize, boardSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(4)
            },
        )

        // FEN row
        val fenRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        fenRow.addView(fenInput, LinearLayout.LayoutParams(0, dp(56), 1f))
        fenRow.addView(
            TrxButton(context, null, designSystem).apply {
                text = resources.getString(R.string.editor_load)
                style = ButtonStyle.SECONDARY
                setOnClickListener {
                    if (viewModel.loadFen(fenInput.text.toString().trim())) {
                        fenError.visibility = View.GONE
                    } else {
                        fenError.visibility = View.VISIBLE
                    }
                }
            },
            LinearLayout.LayoutParams(dp(76), dp(44)).apply { leftMargin = dp(8) },
        )
        content.addView(fenRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)))
        content.addView(
            fenError,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)).apply { leftMargin = dp(12) },
        )

        // side selector
        val sides = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        sides.addView(sideWhite, LinearLayout.LayoutParams(dp(84), dp(40)))
        sides.addView(sideBlack, LinearLayout.LayoutParams(dp(84), dp(40)).apply { leftMargin = dp(8) })
        content.addView(sides, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))

        // piece palette
        PieceType.entries.forEach { type ->
            val btn = PieceButton(context, designSystem, type) {
                viewModel.setPieceType(type)
                refreshPalette()
            }
            pieceButtons += btn
            piecePalette.addView(btn, LinearLayout.LayoutParams(dp(48), dp(52)))
        }
        content.addView(piecePalette, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))

        // action row
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val clearButton = TrxButton(context, null, designSystem).apply {
            text = resources.getString(R.string.editor_clear)
            style = ButtonStyle.GHOST
            setOnClickListener { viewModel.clearBoard() }
        }
        val startButton = TrxButton(context, null, designSystem).apply {
            text = resources.getString(R.string.analysis_controls_new)
            style = ButtonStyle.GHOST
            setOnClickListener { viewModel.startPosition() }
        }
        val analyzeButton = TrxButton(context, null, designSystem).apply {
            text = resources.getString(R.string.analysis_controls_analyze)
            style = ButtonStyle.PRIMARY
            setOnClickListener { onAnalyze(viewModel.currentFen()) }
        }
        actions.addView(clearButton, LinearLayout.LayoutParams(dp(88), dp(44)))
        actions.addView(startButton, LinearLayout.LayoutParams(dp(88), dp(44)).apply { leftMargin = dp(8) })
        actions.addView(analyzeButton, LinearLayout.LayoutParams(dp(100), dp(44)).apply { leftMargin = dp(8) })
        content.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(4) })

        content.addView(
            TextView(context).apply {
                text = resources.getString(R.string.editor_tap_instruction)
                setTextColor(designSystem.colors.textMuted)
                textSize = TypeTokens.Caption.sizeSp
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32)),
        )

        board.interactive = true
        board.onSquareTap = { viewModel.toggleSquare(it) }

        scope.launch {
            viewModel.state.collect { state ->
                board.setPosition(state.position)
                board.theme = BoardThemes.byId(container.settings.settings.value.boardThemeId)
                board.setSelected(null)
                board.setLegalTargets(emptySet())
                fenInput.setText(state.fenText)
                fenInput.setSelection(state.fenText.length)
                if (state.fenError != null) {
                    fenError.text = state.fenError
                    fenError.visibility = View.VISIBLE
                }
                refreshPalette()
            }
        }
    }

    private fun refreshPalette() {
        val selected = viewModel.state.value.selectedPiece
        pieceButtons.forEach { btn ->
            btn.checked = btn.type == selected.type
            btn.side = selected.side
        }
        sideWhite.style = if (selected.side == com.troxzy.trxchess.chess.Side.WHITE) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY
        sideBlack.style = if (selected.side == com.troxzy.trxchess.chess.Side.BLACK) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    private fun dp(v: Int): Int = (v * dpF).toInt()
}

private class PieceButton(
    context: Context,
    private val designSystem: DesignSystem,
    val type: PieceType,
    private val onClick: () -> Unit,
) : View(context) {

    var checked: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var side: com.troxzy.trxchess.chess.Side = com.troxzy.trxchess.chess.Side.WHITE
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        isClickable = true
        isFocusable = true
        contentDescription = type.name.lowercase()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(48f).toInt(), widthMeasureSpec),
            resolveSize(dp(52f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()

        bgPaint.color = if (checked) c.primaryDim else c.surfaceElevated
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, dp(10f), dp(10f), bgPaint)

        glyphPaint.textSize = dp(28f)
        glyphPaint.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        glyphPaint.color = if (side == com.troxzy.trxchess.chess.Side.WHITE) Color.rgb(248, 250, 252) else Color.rgb(20, 22, 26)
        glyphPaint.textAlign = Paint.Align.CENTER
        val baseline = h / 2f - (glyphPaint.descent() + glyphPaint.ascent()) / 2f
        canvas.drawText(glyph(), w / 2f, baseline, glyphPaint)
    }

    private fun glyph(): String = when (type) {
        PieceType.PAWN -> "\u2659"
        PieceType.KNIGHT -> "\u2658"
        PieceType.BISHOP -> "\u2657"
        PieceType.ROOK -> "\u2656"
        PieceType.QUEEN -> "\u2655"
        PieceType.KING -> "\u2654"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            onClick()
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}