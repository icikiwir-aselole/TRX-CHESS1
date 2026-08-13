package com.troxzy.trxchess.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.troxzy.trxchess.R
import com.troxzy.trxchess.chess.PieceType
import com.troxzy.trxchess.chess.Side
import com.troxzy.trxchess.ui.components.ButtonStyle
import com.troxzy.trxchess.ui.components.TrxButton
import com.troxzy.trxchess.ui.designsystem.DesignSystem

/**
 * Promotion chooser. Pure presentation: which moves are promotions is decided
 * by the view model from the rules; this view only renders the four options
 * for the side about to promote. Theme-aware, accessible (content
 * descriptions, keyboard focusable) and usable with reduced motion.
 */
class PromotionDialogView(
    context: Context,
    private val designSystem: DesignSystem,
    private val onPick: (PieceType) -> Unit,
    private val onCancel: () -> Unit,
) {
    private val dialog = Dialog(context)

    private val whiteGlyphs = charArrayOf('♕', '♖', '♗', '♘')
    private val blackGlyphs = charArrayOf('♛', '♜', '♝', '♞')
    private val types = arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
    private val labels = intArrayOf(R.string.promotion_queen, R.string.promotion_rook, R.string.promotion_bishop, R.string.promotion_knight)
    private val pieceButtons = mutableListOf<TextView>()

    init {
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { onCancel() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dpF = context.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * dpF).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(designSystem.colors.surfaceElevated)
            }
        }

        root.addView(
            TextView(context).apply {
                text = context.getString(R.string.promotion_title)
                setTextColor(designSystem.colors.textPrimary)
                textSize = 16f
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)),
        )

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in types.indices) {
            val btn = pieceButton(context, i)
            pieceButtons += btn
            row.addView(
                btn,
                LinearLayout.LayoutParams(dp(64), dp(64)).apply { if (i > 0) leftMargin = dp(8) },
            )
        }
        root.addView(
            row,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(8) },
        )

        root.addView(
            TrxButton(context, null, designSystem).apply {
                text = context.getString(R.string.promotion_cancel)
                style = ButtonStyle.GHOST
                setOnClickListener { dismiss() }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(12) },
        )

        dialog.setContentView(root)
        root.post {
            dialog.window?.setLayout(dp(296), ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setGravity(Gravity.CENTER)
        }
    }

    fun show(side: Side) {
        if (dialog.isShowing) return
        val glyphs = if (side == Side.WHITE) whiteGlyphs else blackGlyphs
        for (i in pieceButtons.indices) pieceButtons[i].text = glyphs[i].toString()
        dialog.show()
    }

    fun dismiss() {
        if (dialog.isShowing) dialog.dismiss()
    }

    private fun pieceButton(context: Context, index: Int): TextView =
        TextView(context).apply {
            text = whiteGlyphs[index].toString()
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(designSystem.colors.textPrimary)
            contentDescription = context.getString(labels[index])
            isFocusable = true
            isFocusableInTouchMode = false
            background = GradientDrawable().apply {
                cornerRadius = (16 * context.resources.displayMetrics.density).toInt().toFloat()
                setColor(designSystem.colors.primaryDim)
            }
            setOnClickListener { onPick(types[index]); dismiss() }
        }
}