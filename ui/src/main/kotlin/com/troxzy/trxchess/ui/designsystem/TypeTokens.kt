package com.troxzy.trxchess.ui.designsystem

import android.graphics.Typeface

/**
 * Typographic roles.
 *
 * Uses the platform sans-serif family with bold variants for display/headline
 * and monospace for diagnostics/engine metrics. System font scaling and
 * TalkBack work out of the box because no fixed physical font files are used.
 */
data class TypeRole(
    val sizeSp: Float,
    val lineHeightSp: Float,
    val typeface: Typeface,
    val letterSpacing: Float = 0f,
)

object TypeTokens {
    private val sans = "sans-serif"
    val Display = TypeRole(34f, 40f, Typeface.create(sans, Typeface.BOLD))
    val Headline = TypeRole(24f, 30f, Typeface.create(sans, Typeface.BOLD))
    val Title = TypeRole(18f, 24f, Typeface.create(sans, Typeface.BOLD))
    val Body = TypeRole(15f, 21f, Typeface.create(sans, Typeface.NORMAL))
    val BodyStrong = TypeRole(15f, 21f, Typeface.create(sans, Typeface.BOLD))
    val Label = TypeRole(13f, 18f, Typeface.create(sans, Typeface.BOLD))
    val Caption = TypeRole(12f, 16f, Typeface.create(sans, Typeface.NORMAL))
    val Mono = TypeRole(13f, 18f, Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL))
    val MonoBold = TypeRole(15f, 20f, Typeface.create(Typeface.MONOSPACE, Typeface.BOLD))
    val MonoSmall = TypeRole(11f, 15f, Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL))
}
