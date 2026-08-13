package com.troxzy.trxchess

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.brand.KnightView
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens

/**
 * Brand splash: knight reveal with sweep animation, then hands off to the
 * main activity. Honors reduced motion; never blocks on work.
 */
class SplashActivity : Activity() {

    private var animator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TrxApp).container
        val designSystem: DesignSystem = container.designSystem

        val root = FrameLayout(this)
        root.setBackgroundColor(designSystem.colors.surface)

        val knight = KnightView(this, null, designSystem)
        val size = dp(120)
        root.addView(
            knight,
            FrameLayout.LayoutParams(size, size).apply {
                leftMargin = (resources.displayMetrics.widthPixels - size) / 2
                topMargin = (resources.displayMetrics.heightPixels - size) / 2 - dp(40)
            },
        )

        val wordmark = TextView(this).apply {
            text = resources.getString(R.string.app_name)
            setTextColor(designSystem.colors.textPrimary)
            textSize = TypeTokens.Display.sizeSp
            typeface = TypeTokens.Display.typeface
        }
        root.addView(
            wordmark,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (resources.displayMetrics.widthPixels - dp(200)) / 2
                topMargin = (resources.displayMetrics.heightPixels) / 2 + dp(70)
            },
        )

        setContentView(root)

        if (designSystem.visualPolicy.motionEnabled) {
            knight.alpha = 0f
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = designSystem.scaledDuration(
                    com.troxzy.trxchess.ui.designsystem.AnimationCategory.CINEMATIC,
                    520L,
                )
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    knight.alpha = it.animatedValue as Float
                    wordmark.alpha = (it.animatedValue as Float).coerceAtMost(0.9f)
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        launchMain()
                    }
                })
                start()
            }
        } else {
            root.postDelayed({ launchMain() }, 350L)
        }
    }

    private fun launchMain() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        animator?.cancel()
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}