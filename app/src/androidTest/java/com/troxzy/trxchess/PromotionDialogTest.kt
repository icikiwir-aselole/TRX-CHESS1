package com.troxzy.trxchess

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.troxzy.trxchess.ui.board.BoardView
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Promotion flow on a real device: moving a pawn to the last rank opens the
 * chooser instead of auto-queening; picking a piece applies it; canceling
 * leaves the board untouched. The dialog is rendered by the app (custom
 * window), so dialog-root matchers are used.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PromotionDialogTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun string(resId: Int): String = ctx.getString(resId)

    private fun openAnalysisWithPromotionFen() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
        onView(withHint(string(R.string.analysis_fen_hint)))
            .perform(scrollTo(), replaceText("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"))
        onView(withContentDescription(string(R.string.analysis_fen_load)))
            .perform(scrollTo(), click())
    }

    @Test
    fun promotionShowsDialogAndPickingQueenAppliesIt() {
        openAnalysisWithPromotionFen()

        // a7 -> a8
        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 6))
        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 7))

        TestHelpers.waitForView(withText(string(R.string.promotion_title)))
        TestHelpers.waitForView(
            withContentDescription(string(R.string.promotion_queen)),
        )

        onView(withContentDescription(string(R.string.promotion_queen))).perform(click())

        // White promotes to queen; black king in check along rank 8.
        TestHelpers.waitForText(string(R.string.position_status_check))
    }

    @Test
    fun cancelingPromotionLeavesBoardUntouched() {
        openAnalysisWithPromotionFen()

        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 6))
        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 7))

        TestHelpers.waitForView(withText(string(R.string.promotion_title)))

        androidx.test.espresso.Espresso.pressBack()

        TestHelpers.assertAbsent(withText(string(R.string.promotion_title)))
        TestHelpers.assertAbsent(withText(string(R.string.position_status_check)))
        TestHelpers.assertAbsent(withText(string(R.string.position_status_checkmate)))
    }

    @Test
    fun rapidPromotionTapsDoNotCrash() {
        openAnalysisWithPromotionFen()

        // Rapid taps: selection then target twice; only one dialog may open.
        for (i in 1..3) {
            onView(withClassName(equalTo(BoardView::class.java.name)))
                .perform(TestHelpers.boardTap(0, 6))
            onView(withClassName(equalTo(BoardView::class.java.name)))
                .perform(TestHelpers.boardTap(0, 7))
        }

        TestHelpers.waitForView(withText(string(R.string.promotion_title)))
        onView(withText(string(R.string.promotion_title))).check(
            androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()),
        )
        androidx.test.espresso.Espresso.pressBack()
        TestHelpers.waitForText(string(R.string.analysis_title))
    }

    @Test
    fun blackPromotionShowsChooserAndAppliesKnight() {
        // Black to move: pawn b2 -> b1 with promotion chooser.
        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
        onView(withHint(string(R.string.analysis_fen_hint)))
            .perform(scrollTo(), replaceText("4k3/8/8/8/8/8/1p6/4K3 b - - 0 1"))
        onView(withContentDescription(string(R.string.analysis_fen_load)))
            .perform(scrollTo(), click())

        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(1, 1))
        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(1, 0))

        TestHelpers.waitForView(withText(string(R.string.promotion_title)))
        TestHelpers.waitForView(
            withContentDescription(string(R.string.promotion_knight)),
        )

        onView(withContentDescription(string(R.string.promotion_knight))).perform(click())

        // Black knight on b1; dialog gone, board still interactive (no crash).
        TestHelpers.assertAbsent(withText(string(R.string.promotion_title)))
        TestHelpers.waitForView(withClassName(equalTo(BoardView::class.java.name)))
    }

    @Test
    fun promotionCaptureAppliesAndRegistersCheck() {
        // White pawn a7 captures the rook on b8, promoting to queen; queen
        // on b8 checks the black king on c8.
        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
        onView(withHint(string(R.string.analysis_fen_hint)))
            .perform(scrollTo(), replaceText("1rk5/P7/8/8/8/8/8/4K3 w - - 0 1"))
        onView(withContentDescription(string(R.string.analysis_fen_load)))
            .perform(scrollTo(), click())

        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 6))
        onView(withClassName(equalTo(BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(1, 7))

        TestHelpers.waitForView(withText(string(R.string.promotion_title)))
        onView(withContentDescription(string(R.string.promotion_queen))).perform(click())

        TestHelpers.waitForText(string(R.string.position_status_check))
    }
}