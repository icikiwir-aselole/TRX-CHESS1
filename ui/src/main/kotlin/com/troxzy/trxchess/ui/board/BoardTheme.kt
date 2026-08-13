package com.troxzy.trxchess.ui.board

import android.graphics.Color

/**
 * Board visual theme. Themes never affect chess rules — they only supply
 * colors used by [BoardView].
 */
data class BoardTheme(
    val id: String,
    val name: String,
    val light: Int,
    val dark: Int,
    val selected: Int,
    val lastMove: Int,
    val legalDot: Int,
    val check: Int,
    val coordinate: Int,
)

object BoardThemes {

    val Classic = BoardTheme(
        id = "classic", name = "Classic",
        light = Color.rgb(238, 221, 192), dark = Color.rgb(122, 101, 75),
        selected = Color.argb(150, 224, 48, 64), lastMove = Color.argb(120, 200, 170, 90),
        legalDot = Color.argb(120, 40, 46, 54), check = Color.argb(200, 255, 45, 58),
        coordinate = Color.argb(150, 16, 20, 24),
    )

    val Crimson = BoardTheme(
        id = "crimson", name = "Crimson",
        light = Color.rgb(44, 18, 22), dark = Color.rgb(92, 26, 34),
        selected = Color.argb(170, 255, 62, 78), lastMove = Color.argb(140, 224, 48, 64),
        legalDot = Color.argb(150, 242, 245, 247), check = Color.argb(220, 255, 220, 224),
        coordinate = Color.argb(160, 255, 210, 216),
    )

    val Carbon = BoardTheme(
        id = "carbon", name = "Carbon",
        light = Color.rgb(34, 38, 46), dark = Color.rgb(22, 25, 31),
        selected = Color.argb(160, 224, 48, 64), lastMove = Color.argb(120, 64, 74, 88),
        legalDot = Color.argb(150, 200, 208, 216), check = Color.argb(200, 255, 45, 58),
        coordinate = Color.argb(140, 200, 208, 216),
    )

    val Neon = BoardTheme(
        id = "neon", name = "Neon",
        light = Color.rgb(16, 22, 28), dark = Color.rgb(10, 42, 48),
        selected = Color.argb(170, 124, 196, 255), lastMove = Color.argb(130, 0, 200, 220),
        legalDot = Color.argb(170, 124, 196, 255), check = Color.argb(220, 255, 45, 58),
        coordinate = Color.argb(160, 124, 196, 255),
    )

    val Custom = BoardTheme(
        id = "custom", name = "Custom",
        light = Color.rgb(50, 44, 38), dark = Color.rgb(30, 26, 22),
        selected = Color.argb(150, 224, 48, 64), lastMove = Color.argb(110, 180, 150, 90),
        legalDot = Color.argb(140, 240, 244, 248), check = Color.argb(200, 255, 45, 58),
        coordinate = Color.argb(140, 230, 220, 200),
    )

    val all = listOf(Classic, Crimson, Carbon, Neon, Custom)

    fun byId(id: String): BoardTheme = all.firstOrNull { it.id == id } ?: Classic
}
