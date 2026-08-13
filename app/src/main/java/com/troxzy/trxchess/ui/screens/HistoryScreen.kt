package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.troxzy.trxchess.R
import com.troxzy.trxchess.data.AnalysisSessionEntity
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.di.HistoryStore
import com.troxzy.trxchess.ui.common.TopBarView
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * History screen: saved analysis sessions from Room. Strings come from
 * resources; empty state is explicit.
 */
class HistoryScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    private val onOpenSession: (AnalysisSessionEntity) -> Unit,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val activity = context as ComponentActivity
    private val viewModel: HistoryViewModel = ViewModelProvider(
        activity,
        HistoryViewModel.factory(container.history),
    )[HistoryViewModel::class.java]

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val scroll = ScrollView(context)
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.history_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })
        scroll.addView(content)

        scope.launch {
            viewModel.state.collect { render(it) }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    private fun render(state: HistoryUiState) {
        content.removeAllViews()
        if (state.sessions.isEmpty()) {
            val empty = TextView(context).apply {
                text = resources.getString(R.string.history_empty)
                setTextColor(designSystem.colors.textMuted)
                textSize = TypeTokens.Body.sizeSp
                gravity = Gravity.CENTER
            }
            content.addView(empty, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)))
        } else {
            state.sessions.forEach { session ->
                content.addView(
                    SessionCard(context, designSystem, session) { onOpenSession(session) },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)).apply {
                        leftMargin = dp(16)
                        rightMargin = dp(16)
                        bottomMargin = dp(10)
                    },
                )
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

data class HistoryUiState(val sessions: List<AnalysisSessionEntity> = emptyList())

class HistoryViewModel(private val store: HistoryStore) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.sessions.collect { sessions ->
                _state.value = HistoryUiState(sessions)
            }
        }
    }

    companion object {
        fun factory(store: HistoryStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(store) as T
        }
    }
}

private class SessionCard(
    context: Context,
    private val designSystem: DesignSystem,
    private val session: AnalysisSessionEntity,
    private val onClick: () -> Unit,
) : View(context) {

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var pressed = false

    init {
        isClickable = true
        isFocusable = true
        contentDescription = session.name
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(280f).toInt(), widthMeasureSpec),
            resolveSize(dp(72f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = dp(14f)

        cardPaint.color = c.surfaceElevated
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, cardPaint)

        titlePaint.textSize = TypeTokens.BodyStrong.sizeSp * resources.displayMetrics.scaledDensity
        titlePaint.typeface = TypeTokens.BodyStrong.typeface
        titlePaint.color = c.textPrimary
        titlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(session.name, dp(16f), dp(28f), titlePaint)

        metaPaint.textSize = TypeTokens.Caption.sizeSp * resources.displayMetrics.scaledDensity
        metaPaint.typeface = TypeTokens.Caption.typeface
        metaPaint.color = c.textMuted
        metaPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(session.initialFen, dp(16f), dp(50f), metaPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                pressed = false
                invalidate()
                onClick()
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}