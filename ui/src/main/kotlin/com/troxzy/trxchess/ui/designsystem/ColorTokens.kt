package com.troxzy.trxchess.ui.designsystem

/**
 * Central TRX-CHESS color palette.
 *
 * Primary identity: near-black, deep gray, white/silver and neon crimson.
 * Every surface derives from these tokens; screens must not hard-code colors.
 */
data class TrxColors(
    val background: Int,
    val backgroundElevated: Int,
    val surface: Int,
    val surfaceElevated: Int,
    val surfaceGlass: Int,
    val primary: Int,
    val primaryGlow: Int,
    val primaryDim: Int,
    val secondary: Int,
    val accent: Int,
    val danger: Int,
    val warning: Int,
    val success: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textMuted: Int,
    val divider: Int,
) {
    companion object {
        val Dark = TrxColors(
            background = 0xFF0A0D12.toInt(),
            backgroundElevated = 0xFF0E1218.toInt(),
            surface = 0xFF11151C.toInt(),
            surfaceElevated = 0xFF161B24.toInt(),
            surfaceGlass = 0x1F1A202A.toInt(),
            primary = 0xFFE03040.toInt(),
            primaryGlow = 0xFFFF3B4D.toInt(),
            primaryDim = 0xFF5A1620.toInt(),
            secondary = 0xFFDDE3EA.toInt(),
            accent = 0xFF7CC4FF.toInt(),
            danger = 0xFFFF2D3A.toInt(),
            warning = 0xFFFFB020.toInt(),
            success = 0xFF3DD68C.toInt(),
            textPrimary = 0xFFF2F5F7.toInt(),
            textSecondary = 0xFFB8C2CC.toInt(),
            textMuted = 0xFF6E7A86.toInt(),
            divider = 0xFF1E2530.toInt(),
        )

        val Light = TrxColors(
            background = 0xFFF2F4F7.toInt(),
            backgroundElevated = 0xFFE9ECF1.toInt(),
            surface = 0xFFFFFFFF.toInt(),
            surfaceElevated = 0xFFF7F8FA.toInt(),
            surfaceGlass = 0xB8FFFFFF.toInt(),
            primary = 0xFFC22030.toInt(),
            primaryGlow = 0xFFE03040.toInt(),
            primaryDim = 0xFFF7DCE0.toInt(),
            secondary = 0xFF23303C.toInt(),
            accent = 0xFF1F6FB2.toInt(),
            danger = 0xFFC81E2B.toInt(),
            warning = 0xFF9A6A00.toInt(),
            success = 0xFF1F8A5A.toInt(),
            textPrimary = 0xFF101418.toInt(),
            textSecondary = 0xFF3A4550.toInt(),
            textMuted = 0xFF7A8691.toInt(),
            divider = 0xFFD9DEE5.toInt(),
        )
    }
}

enum class ThemeMode { DARK, LIGHT, SYSTEM }
