package com.troxzy.trxchess.ui.designsystem

import android.graphics.RectF

object Spacing {
    val Xxs = 2
    val Xs = 4
    val Sm = 8
    val Md = 12
    val Lg = 16
    val Xl = 24
    val Xxl = 32
    val Xxxl = 48
}

object Radius {
    val Small = 8f
    val Medium = 12f
    val Large = 16f
    val Xl = 24f
    val Pill = 999f
}

object Elevation {
    val Card = 6f
    val Raised = 12f
    val Overlay = 24f
}

/** Convenience round-rect builder for drawables/canvas. */
fun roundedRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float): RectF =
    RectF(left, top, right, bottom)
