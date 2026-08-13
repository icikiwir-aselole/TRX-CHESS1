package com.troxzy.trxchess

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** FEN import on the analysis screen: valid loads, invalid shows categorized errors. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FenImportTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun string(resId: Int): String = ctx.getString(resId)

    private fun openAnalysis() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
    }

    @Test
    fun validCheckmateFenShowsCheckmateStatus() {
        openAnalysis()
        val fen = "4R2k/5ppp/8/8/8/8/8/4K3 b - - 0 1"
        onView(withHint(string(R.string.analysis_fen_hint))).perform(scrollTo(), replaceText(fen))
        onView(withContentDescription(string(R.string.analysis_fen_load))).perform(scrollTo(), click())

        TestHelpers.waitForText(string(R.string.position_status_checkmate))
        TestHelpers.assertAbsent(withText(string(R.string.fen_error_board)))
    }

    @Test
    fun validCheckPositionShowsCheckStatus() {
        openAnalysis()
        val fen = "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"
        onView(withHint(string(R.string.analysis_fen_hint))).perform(scrollTo(), replaceText(fen))
        onView(withContentDescription(string(R.string.analysis_fen_load))).perform(scrollTo(), click())

        TestHelpers.waitForText(string(R.string.position_status_check))
    }

    @Test
    fun malformedFenShowsCategorizedError() {
        openAnalysis()
        onView(withHint(string(R.string.analysis_fen_hint))).perform(scrollTo(), replaceText("not a fen"))
        onView(withContentDescription(string(R.string.analysis_fen_load))).perform(scrollTo(), click())

        TestHelpers.waitForText(string(R.string.fen_error_field_count))
    }

    @Test
    fun invalidPieceShowsPieceError() {
        openAnalysis()
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQXBNR w KQkq - 0 1"
        onView(withHint(string(R.string.analysis_fen_hint))).perform(scrollTo(), replaceText(fen))
        onView(withContentDescription(string(R.string.analysis_fen_load))).perform(scrollTo(), click())

        TestHelpers.waitForText(string(R.string.fen_error_piece))
    }

    @Test
    fun clearButtonEmptiesTheField() {
        openAnalysis()
        onView(withHint(string(R.string.analysis_fen_hint))).perform(scrollTo(), replaceText("8/8/8/8/8/8/8/8 w - - 0 1"))
        onView(withContentDescription(string(R.string.analysis_fen_clear))).perform(scrollTo(), click())

        TestHelpers.waitForView(withHint(string(R.string.analysis_fen_hint)))
        onView(withHint(string(R.string.analysis_fen_hint)))
            .check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("")))
    }

    @Test
    fun rapidRepeatedLoadsSettleOnLastSubmittedFen() {
        openAnalysis()
        val checkmateFen = "4R2k/5ppp/8/8/8/8/8/4K3 b - - 0 1"
        val checkFen = "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"
        // Rapidly replace + load several times; the last submitted load must
        // win even if background parsing completes out of order.
        repeat(3) {
            onView(withHint(string(R.string.analysis_fen_hint)))
                .perform(scrollTo(), replaceText(if (it % 2 == 0) checkmateFen else checkFen))
            onView(withContentDescription(string(R.string.analysis_fen_load)))
                .perform(scrollTo(), click())
        }

        TestHelpers.waitForText(string(R.string.position_status_checkmate))
        TestHelpers.assertAbsent(withText(string(R.string.position_status_check)))
    }
}