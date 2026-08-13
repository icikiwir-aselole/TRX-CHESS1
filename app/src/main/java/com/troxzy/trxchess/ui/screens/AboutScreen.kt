package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.troxzy.trxchess.BuildConfig
import com.troxzy.trxchess.R
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.brand.KnightView
import com.troxzy.trxchess.ui.common.SettingRowView
import com.troxzy.trxchess.ui.common.TopBarView
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens

/**
 * About screen: branding, version, links and licenses. All strings from
 * resources.
 */
class AboutScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val activity = context as ComponentActivity

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.about_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }
        addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })

        val knight = KnightView(context, null, designSystem)
        root.addView(knight, LinearLayout.LayoutParams(dp(96), dp(96)))

        root.addView(TextView(context).apply {
            text = resources.getString(R.string.app_name)
            setTextColor(designSystem.colors.textPrimary)
            textSize = TypeTokens.Display.sizeSp
            typeface = TypeTokens.Display.typeface
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })

        root.addView(TextView(context).apply {
            text = resources.getString(R.string.about_author)
            setTextColor(designSystem.colors.textMuted)
            textSize = TypeTokens.Body.sizeSp
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        root.addView(TextView(context).apply {
            text = resources.getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
            setTextColor(designSystem.colors.textSecondary)
            textSize = TypeTokens.Caption.sizeSp
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        root.addView(TextView(context).apply {
            text = resources.getString(R.string.about_offline_first)
            setTextColor(designSystem.colors.textMuted)
            textSize = TypeTokens.Caption.sizeSp
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)).apply { bottomMargin = dp(16) })

        val telegram = SettingRowView(context, null, designSystem).apply {
            label = resources.getString(R.string.about_telegram)
            chevron = true
            onTap = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SoloBanNoTrash"))
                    activity.startActivity(intent)
                }
            }
        }
        root.addView(telegram, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        val licenses = SettingRowView(context, null, designSystem).apply {
            label = resources.getString(R.string.about_licenses)
            chevron = true
            onTap = {
                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))) }
            }
        }
        root.addView(licenses, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}