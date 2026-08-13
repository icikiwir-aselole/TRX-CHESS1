package com.troxzy.trxchess

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Activity recreation: the app must survive config/process-style recreation. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RecreationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun string(resId: Int): String = ctx.getString(resId)

    @Test
    fun recreationOnHomeReturnsToHome() {
        TestHelpers.waitForText(string(R.string.home_title))
        activityRule.scenario.recreate()
        TestHelpers.waitForText(string(R.string.home_title))
    }

    @Test
    fun recreationWhileOnSettingsReturnsWithoutCrash() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_settings)))
        onView(withContentDescription(string(R.string.home_settings)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.settings_title))

        activityRule.scenario.recreate()

        // The in-memory back stack resets to Home on full recreation;
        // the app must land there without crashing.
        TestHelpers.waitForText(string(R.string.home_title))
    }

    @Test
    fun navigationWorksAfterRecreation() {
        TestHelpers.waitForText(string(R.string.home_title))
        activityRule.scenario.recreate()
        TestHelpers.waitForText(string(R.string.home_title))

        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
    }

    @Test
    fun recreationDuringPromotionDialogLandsOnHomeWithoutCrash() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
        onView(androidx.test.espresso.matcher.ViewMatchers.withHint(string(R.string.analysis_fen_hint)))
            .perform(scrollTo(), androidx.test.espresso.action.ViewActions.replaceText("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"))
        onView(withContentDescription(string(R.string.analysis_fen_load)))
            .perform(scrollTo(), click())

        onView(androidx.test.espresso.matcher.ViewMatchers.withClassName(org.hamcrest.Matchers.equalTo(com.troxzy.trxchess.ui.board.BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 6))
        onView(androidx.test.espresso.matcher.ViewMatchers.withClassName(org.hamcrest.Matchers.equalTo(com.troxzy.trxchess.ui.board.BoardView::class.java.name)))
            .perform(TestHelpers.boardTap(0, 7))

        TestHelpers.waitForText(string(R.string.promotion_title))

        activityRule.scenario.recreate()

        // Dialog belongs to the recreated activity; the app must land on
        // Home without crashing and without a dangling dialog.
        TestHelpers.waitForText(string(R.string.home_title))
        TestHelpers.assertAbsent(
            androidx.test.espresso.matcher.ViewMatchers.withText(string(R.string.promotion_title)),
        )
    }
}