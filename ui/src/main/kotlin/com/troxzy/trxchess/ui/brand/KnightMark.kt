package com.troxzy.trxchess.ui.brand

import android.graphics.Path

/**
 * TRX Knight silhouette.
 *
 * Built from the classic chess knight outline (public-domain geometry) scaled
 * to a unit box (0..100). Screens/views scale and style this path; keeping the
 * geometry here guarantees a consistent, recognizable silhouette at any size.
 */
object KnightMark {

    private const val S = 100f / 45f

    private fun Path.curve(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) {
        cubicTo(a * S, b * S, c * S, d * S, e * S, f * S)
    }

    private fun Path.seg(a: Float, b: Float) {
        lineTo(a * S, b * S)
    }

    /** Knight head/neck silhouette facing left, in the 0..100 unit box. */
    fun silhouette(): Path {
        val p = Path()
        p.moveTo(24.55f * S, 10.03f * S)
        p.curve(21.06f, 10.03f, 19.24f, 12.55f, 19.19f, 14.53f)
        p.curve(19.12f, 17.22f, 21.18f, 18.3f, 21.18f, 18.3f)
        p.seg(19.69f, 19.61f)
        p.curve(15.35f, 23.12f, 16.17f, 27.8f, 16.17f, 27.8f)
        p.curve(15.3f, 27.75f, 15.75f, 29.4f, 15.75f, 29.4f)
        p.curve(15.75f, 29.4f, 13.75f, 30.3f, 13.75f, 30.3f)
        p.curve(12.85f, 29.3f, 11.53f, 29.85f, 11.53f, 29.85f)
        p.curve(12.35f, 27.5f, 13.56f, 25.63f, 14.51f, 24.26f)
        p.curve(14.25f, 23.53f, 13.14f, 23.22f, 13.14f, 23.22f)
        p.curve(12.05f, 25.18f, 10.09f, 27.85f, 9.06f, 30.64f)
        p.curve(8.4f, 32.5f, 10.05f, 34.15f, 11.8f, 34.15f)
        p.curve(13.53f, 34.15f, 13.94f, 32.53f, 13.94f, 32.53f)
        p.curve(14.32f, 33.79f, 13.2f, 35.63f, 13.71f, 36.72f)
        p.curve(14.16f, 37.64f, 15.65f, 37.56f, 15.96f, 36.62f)
        p.curve(16.24f, 35.8f, 16.79f, 35.3f, 17.33f, 34.72f)
        p.curve(17.69f, 34.38f, 18.08f, 34.02f, 18.41f, 33.65f)
        p.curve(18.29f, 35.12f, 18.24f, 37.11f, 18.42f, 38.04f)
        p.curve(18.54f, 38.63f, 19.29f, 39.16f, 19.87f, 38.57f)
        p.curve(20.58f, 37.86f, 20.39f, 37.11f, 20.6f, 36.4f)
        p.curve(20.93f, 35.19f, 21.91f, 34.73f, 23.04f, 33.98f)
        p.curve(25.13f, 32.65f, 27.33f, 32.13f, 29.5f, 32.13f)
        p.curve(31.17f, 32.13f, 32.6f, 32.13f, 33.54f, 31.63f)
        p.curve(33.54f, 32.72f, 34.19f, 33.8f, 35.27f, 33.8f)
        p.curve(36.13f, 33.8f, 36.55f, 32.95f, 36.55f, 32.95f)
        p.seg(37.14f, 33.53f)
        p.curve(37.54f, 34.8f, 37.5f, 36.15f, 37.5f, 36.15f)
        p.curve(39.57f, 35.24f, 39.7f, 32.83f, 39.7f, 32.83f)
        p.curve(40.17f, 31.13f, 40.2f, 29.68f, 39.5f, 27.6f)
        p.curve(38.84f, 25.65f, 38.14f, 24.02f, 37.48f, 22.53f)
        p.curve(39.18f, 21.82f, 39.94f, 19.67f, 39.94f, 19.67f)
        p.curve(41.14f, 17.48f, 39.5f, 14.34f, 39.5f, 14.34f)
        p.curve(38.6f, 13.9f, 37.9f, 14.18f, 37.9f, 14.18f)
        p.curve(38.6f, 12.95f, 38.5f, 10.32f, 38.5f, 10.32f)
        p.curve(36.95f, 10.2f, 36.33f, 11.5f, 36.33f, 11.5f)
        p.curve(35.04f, 10.62f, 33.9f, 10.6f, 33.9f, 10.6f)
        p.curve(33.1f, 8.3f, 29.7f, 8f, 29.7f, 8f)
        p.curve(28.55f, 8f, 27.05f, 8.5f, 25.7f, 9.3f)
        p.curve(25.3f, 9.15f, 24.9f, 9.03f, 24.55f, 10.03f)
        p.close()
        return p
    }

    /**
     * Aggressive blade slash behind the knight: a thin angular path from the
     * upper-right across to the lower-left, used as a subtle battle fragment.
     */
    fun slash(): Path = Path().apply {
        moveTo(70f, 16f)
        lineTo(80f, 22f)
        lineTo(46f, 58f)
        lineTo(36f, 52f)
        close()
    }

    /** Compact square "battle fragment" near the base. */
    fun fragment(): Path = Path().apply {
        moveTo(58f, 78f)
        lineTo(66f, 72f)
        lineTo(74f, 84f)
        lineTo(66f, 88f)
        close()
    }
}
