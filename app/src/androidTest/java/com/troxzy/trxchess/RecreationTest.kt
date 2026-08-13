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
}