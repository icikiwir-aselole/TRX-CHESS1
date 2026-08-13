package com.troxzy.trxchess

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matcher

/**
 * Shared helpers for instrumented tests. All assertions here are real:
 * nothing is marked as run unless a device actually executed it.
 */
object TestHelpers {

    /** Polls until [condition] holds or the timeout elapses; fails otherwise. */
    fun waitUntil(timeoutMs: Long = 8000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Condition not met within ${timeoutMs}ms")
    }

    /** Polls until a view matching [matcher] is displayed. */
    fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 8000) {
        waitUntil(timeoutMs) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                true
            } catch (e: NoMatchingViewException) {
                false
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /** Waits until text [text] is visible. */
    fun waitForText(text: String, timeoutMs: Long = 8000) {
        waitForView(withText(text), timeoutMs)
    }

    /** Asserts a view matching [matcher] is absent from the hierarchy. */
    fun assertAbsent(matcher: Matcher<View>, timeoutMs: Long = 3000) {
        waitUntil(timeoutMs) {
            try {
                onView(matcher).check(doesNotExist())
                true
            } catch (e: NoMatchingViewException) {
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /**
     * Taps [view] at a point given in view-local coordinates (fraction of
     * width/height). Used for custom-drawn views such as the board.
     */
    fun tapAt(view: View, xFraction: Float, yFraction: Float) {
        val x = view.width * xFraction
        val y = view.height * yFraction
        val down = MotionEvent.obtain(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN, x, y, 0,
        )
        val up = MotionEvent.obtain(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP, x, y, 0,
        )
        try {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
        } finally {
            down.recycle()
            up.recycle()
        }
    }

    /**
     * Taps a board square in file/rank coordinates (rank 0 = first rank from
     * white's side), then lets the main thread settle.
     */
    fun boardTap(file: Int, rank: Int): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription(): String = "tap board square ($file, $rank)"
        override fun perform(uiController: UiController, view: View) {
            val sq = view.width / 8f
            tapAt(
                view,
                (file * sq + sq / 2f) / view.width,
                ((7 - rank) * sq + sq / 2f) / view.height,
            )
            uiController.loopMainThreadForAtLeast(150)
        }
    }
}