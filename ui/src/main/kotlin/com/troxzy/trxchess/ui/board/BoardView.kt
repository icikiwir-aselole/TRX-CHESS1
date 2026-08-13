package com.troxzy.trxchess.ui.board

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Move
import com.troxzy.trxchess.chess.Piece
import com.troxzy.trxchess.chess.PieceType
import com.troxzy.trxchess.chess.Side
import com.troxzy.trxchess.chess.Square
import com.troxzy.trxchess.ui.designsystem.AnimationCategory
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import kotlin.math.max

/**
 * TRX-CHESS interactive board.
 *
 * Renders the position with last-move / selection / legal-move / check
 * overlays and animates piece movement, captures and promotion. Animation is
 * diff-driven: [setPosition] computes what changed and only animates that,
 * so engine updates never force a full redraw and interrupts are safe.
 *
 * The board is a pure renderer; move legality and state ownership stay in the
 * domain/analysis layer.
 */
class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var theme: BoardTheme = BoardThemes.Carbon
        set(value) {
            field = value
            invalidate()
        }

    var coordinatesVisible: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var flipped: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var interactive: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var showLastMove: Boolean = true
    var showLegalMoves: Boolean = true
    var showCheck: Boolean = true

    var onSquareTap: ((Square) -> Unit)? = null
    var onMovePlayed: ((Move) -> Unit)? = null

    private val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pieceRimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var boardPosition = ChessPosition.start()
    private var selected: Square? = null
    private var lastMove: Move? = null
    private var checkSquare: Square? = null
    private var legalTargets: Set<Square> = emptySet()

    private class AnimPiece(
        val piece: Piece,
        val from: Square,
        val to: Square,
    )

    private var animated = mutableListOf<AnimPiece>()
    private var captureEffect: Pair<Square, Piece>? = null
    private var animator: ValueAnimator? = null
    private var animStartMs = 0L
    private var animDurationMs = 0L

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        contentDescription = "Chess board"
        piecePaint.textAlign = Paint.Align.CENTER
        pieceRimPaint.textAlign = Paint.Align.CENTER
        pieceRimPaint.style = Paint.Style.STROKE
        linePaint.style = Paint.Style.STROKE
    }

    // ---- state API -----------------------------------------------------

    /** Diff-driven position update. Animates only meaningful changes. */
    fun setPosition(new: ChessPosition) {
        val old = boardPosition
        boardPosition = new
        if (!designSystem.visualPolicy.motionEnabled || new.board.size != old.board.size) {
            animated.clear()
            captureEffect = null
            invalidate()
            return
        }
        val moving = findMovingPieces(old, new)
        if (moving.isEmpty()) {
            invalidate()
            return
        }
        animated = moving.toMutableList()
        // captured piece: a square that lost a piece which is not the mover's origin
        val capturedSquare = old.board.keys
            .firstOrNull { s -> new.board[s] == null && moving.none { it.from == s } && old.board[s]?.side != new.sideToMove }
        captureEffect = capturedSquare?.let { s -> s to (old.board[s] ?: return@let null) }
        startMoveAnimation()
    }

    private fun findMovingPieces(old: ChessPosition, new: ChessPosition): List<AnimPiece> {
        val out = mutableListOf<AnimPiece>()
        // piece that disappeared from 'from' and appeared at 'to' with same side/type
        val missing = old.board.filterKeys { it !in new.board }
        val added = new.board.filterKeys { it !in old.board }
        for ((from, piece) in missing) {
            val to = added.entries.firstOrNull { it.value.side == piece.side && it.value.type == piece.type }?.key
            if (to != null) {
                out.add(AnimPiece(piece, from, to))
            }
        }
        return out
    }

    private fun startMoveAnimation() {
        if (animated.isEmpty()) {
            invalidate()
            return
        }
        animator?.cancel()
        animDurationMs = designSystem.scaledDuration(AnimationCategory.STANDARD, 120L)
        animStartMs = SystemClock.uptimeMillis()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animDurationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animated.clear()
                    captureEffect = null
                    invalidate()
                }
            })
            start()
        }
    }

    /** Interrupts any running animation and snaps to the current position. */
    fun cancelAnimation() {
        animator?.cancel()
        animated.clear()
        captureEffect = null
        invalidate()
    }

    override fun onDetachedFromWindow() {
        // Never leak animators when the view leaves the window (theme
        // recreation, navigation, overlay host).
        animator?.cancel()
        animator = null
        animated.clear()
        captureEffect = null
        super.onDetachedFromWindow()
    }

    fun setSelected(square: Square?) {
        selected = square
        invalidate()
    }

    fun setLegalTargets(targets: Set<Square>) {
        legalTargets = targets
        invalidate()
    }

    fun setLastMove(move: Move?) {
        lastMove = move
        invalidate()
    }

    fun setCheckSquare(square: Square?) {
        checkSquare = square
        invalidate()
    }

    // ---- geometry -------------------------------------------------------

    private fun squareSize(): Float = width.coerceAtMost(height).toFloat() / 8f

    private fun squareRect(s: Square): FloatArray {
        val sq = squareSize()
        val col = if (flipped) 7 - s.file else s.file
        val row = if (flipped) s.rank else 7 - s.rank
        return floatArrayOf(col * sq, row * sq)
    }

    private fun squareAt(x: Float, y: Float): Square? {
        val sq = squareSize()
        val col = (x / sq).toInt().coerceIn(0, 7)
        val row = (y / sq).toInt().coerceIn(0, 7)
        val file = if (flipped) 7 - col else col
        val rank = if (flipped) row else 7 - row
        return Square(file, rank)
    }

    // ---- drawing --------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sq = squareSize()
        if (sq <= 0f) return
        val size = sq * 8f
        val offsetX = (width - size) / 2f
        val offsetY = (height - size) / 2f

        drawSquares(canvas, sq, offsetX, offsetY)
        drawLastMove(canvas, sq, offsetX, offsetY)
        drawSelected(canvas, sq, offsetX, offsetY)
        drawLegalMoves(canvas, sq, offsetX, offsetY)
        drawCheck(canvas, sq, offsetX, offsetY)
        drawPieces(canvas, sq, offsetX, offsetY)
        drawCoordinates(canvas, sq, offsetX, offsetY)
    }

    private fun drawSquares(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        for (rank in 0..7) {
            for (file in 0..7) {
                val square = Square(file, rank)
                val (x, y) = squareRect(square)
                squarePaint.color = if ((file + rank) % 2 == 0) theme.light else theme.dark
                canvas.drawRect(ox + x, oy + y, ox + x + sq, oy + y + sq, squarePaint)
            }
        }
        linePaint.color = Color.argb(60, 0, 0, 0)
        linePaint.strokeWidth = dp(1f)
        canvas.drawRect(ox, oy, ox + 8f * sq, oy + 8f * sq, linePaint)
    }

    private fun drawLastMove(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        if (!showLastMove) return
        val m = lastMove ?: return
        overlayPaint.color = theme.lastMove
        for (s in listOf(m.from, m.to)) {
            val (x, y) = squareRect(s)
            canvas.drawRect(ox + x, oy + y, ox + x + sq, oy + y + sq, overlayPaint)
        }
    }

    private fun drawSelected(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        val s = selected ?: return
        val (x, y) = squareRect(s)
        overlayPaint.color = theme.selected
        canvas.drawRect(ox + x, oy + y, ox + x + sq, oy + y + sq, overlayPaint)
        linePaint.color = theme.selected
        linePaint.strokeWidth = dp(2f)
        canvas.drawRect(ox + x, oy + y, ox + x + sq, oy + y + sq, linePaint)
    }

    private fun drawLegalMoves(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        if (!showLegalMoves || legalTargets.isEmpty()) return
        for (target in legalTargets) {
            val (x, y) = squareRect(target)
            val centerX = ox + x + sq / 2f
            val centerY = oy + y + sq / 2f
            val hasPiece = boardPosition.board[target] != null
            if (hasPiece) {
                ringPaint.style = Paint.Style.STROKE
                ringPaint.strokeWidth = dp(2.5f)
                ringPaint.color = theme.legalDot
                canvas.drawCircle(centerX, centerY, sq * 0.28f, ringPaint)
            } else {
                dotPaint.color = theme.legalDot
                canvas.drawCircle(centerX, centerY, sq * 0.14f, dotPaint)
            }
        }
    }

    private fun drawCheck(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        if (!showCheck) return
        val s = checkSquare ?: return
        val (x, y) = squareRect(s)
        overlayPaint.color = theme.check
        canvas.drawRect(ox + x, oy + y, ox + x + sq, oy + y + sq, overlayPaint)
    }

    private fun drawPieces(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        val animatingFrom = animated.map { it.from }.toSet()
        val animatingTo = animated.map { it.to }.toSet()

        // static pieces (skip the ones animating; skip the moving piece's origin)
        for ((square, piece) in boardPosition.board) {
            if (square in animatingTo) continue
            if (animated.any { it.from == square && it.to == square }) continue
            val (x, y) = squareRect(square)
            drawPieceGlyph(canvas, piece, ox + x + sq / 2f, oy + y + sq * 0.88f, sq * 0.92f, 0f, 0f)
        }

        // capture effect (shrinking captured piece at destination)
        captureEffect?.let { (square, piece) ->
            val progress = animator?.animatedValue as? Float ?: 0f
            val (x, y) = squareRect(square)
            val scale = (1f - progress).coerceIn(0f, 1f)
            drawPieceGlyph(canvas, piece, ox + x + sq / 2f, oy + y + sq * 0.88f, sq * 0.92f * scale, 0f, 0f)
        }

        // animated moving pieces
        if (animator != null && animated.isNotEmpty()) {
            val progress = animator?.animatedValue as? Float ?: 1f
            for (ap in animated) {
                val fromRect = squareRect(ap.from)
                val toRect = squareRect(ap.to)
                val cx = lerp(fromRect[0], toRect[0]) * sq + ox + sq / 2f
                val cy = lerp(fromRect[1], toRect[1]) * sq + oy + sq / 2f
                val lift = -max(0f, progress) * sq * 0.28f
                drawPieceGlyph(canvas, ap.piece, cx, cy + lift, sq * 0.92f, lift * 0.1f, lift * 0.05f)
            }
        }
    }

    private fun lerp(a: Float, b: Float): Float {
        val p = animator?.animatedValue as? Float ?: 1f
        return a + (b - a) * p
    }

    private fun drawPieceGlyph(
        canvas: Canvas,
        piece: Piece,
        cx: Float,
        baseline: Float,
        size: Float,
        shadowDx: Float,
        shadowDy: Float,
    ) {
        val isWhite = piece.side == Side.WHITE
        val glyph = glyphFor(piece.type)
        piecePaint.textSize = size
        piecePaint.typeface = Typeface.create("serif", Typeface.BOLD)

        shadowPaint.textSize = size
        shadowPaint.color = Color.argb(120, 0, 0, 0)
        shadowPaint.typeface = piecePaint.typeface
        canvas.drawText(glyph, cx + shadowDx, baseline + shadowDy, shadowPaint)

        piecePaint.color = if (isWhite) Color.rgb(248, 250, 252) else Color.rgb(20, 22, 26)
        canvas.drawText(glyph, cx, baseline, piecePaint)

        pieceRimPaint.textSize = size
        pieceRimPaint.color = if (isWhite) Color.argb(220, 120, 128, 140) else Color.argb(220, 224, 48, 64)
        pieceRimPaint.strokeWidth = max(1f, dp(0.8f))
        canvas.drawText(glyph, cx, baseline, pieceRimPaint)
    }

    private fun glyphFor(type: PieceType): String = when (type) {
        PieceType.PAWN -> "\u2659"
        PieceType.KNIGHT -> "\u2658"
        PieceType.BISHOP -> "\u2657"
        PieceType.ROOK -> "\u2656"
        PieceType.QUEEN -> "\u2655"
        PieceType.KING -> "\u2654"
    }

    private fun drawCoordinates(canvas: Canvas, sq: Float, ox: Float, oy: Float) {
        if (!coordinatesVisible) return
        coordPaint.textSize = sq * 0.28f
        coordPaint.typeface = Typeface.create("monospace", Typeface.BOLD)
        coordPaint.color = theme.coordinate
        val margin = sq * 0.08f
        for (file in 0..7) {
            val (x, _) = squareRect(Square(file, 0))
            coordPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("${('a'.code + file).toChar()}", ox + x + margin, oy + 8f * sq - margin, coordPaint)
        }
        for (rank in 0..7) {
            val (_, y) = squareRect(Square(0, rank))
            coordPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${rank + 1}", ox + 0f + margin * 2f, oy + y + sq - margin * 2f, coordPaint)
        }
    }

    // ---- interaction -----------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val sq = squareAt(event.x, event.y) ?: return true
                val clicked = boardPosition.board[sq]
                if (clicked != null && clicked.side == boardPosition.sideToMove) {
                    onSquareTap?.invoke(sq)
                } else if (sq in legalTargets) {
                    val from = selected ?: return true
                    val move = Move(from, sq)
                    onMovePlayed?.invoke(move)
                } else {
                    onSquareTap?.invoke(sq)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
