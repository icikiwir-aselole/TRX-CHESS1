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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launcher, navigation and back-stack behavior on a real device/emulator. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun string(resId: Int): String = ctx.getString(resId)

    @Test
    fun appLaunchesToHomeAndNavigates() {
        TestHelpers.waitForText(string(R.string.home_title))
        TestHelpers.waitForText(string(R.string.home_subtitle))
    }

    @Test
    fun homeToAnalysisAndBack() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_quick_analysis)))
        onView(withContentDescription(string(R.string.home_quick_analysis)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.analysis_title))
        androidx.test.espresso.Espresso.pressBack()
        TestHelpers.waitForText(string(R.string.home_title))
    }

    @Test
    fun homeToSettingsAndBack() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_settings)))
        onView(withContentDescription(string(R.string.home_settings)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.settings_title))
        androidx.test.espresso.Espresso.pressBack()
        TestHelpers.waitForText(string(R.string.home_title))
    }

    @Test
    fun homeToAboutAndBack() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_about)))
        onView(withContentDescription(string(R.string.home_about)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.about_title))
        TestHelpers.waitForText(string(R.string.about_author))
        androidx.test.espresso.Espresso.pressBack()
        TestHelpers.waitForText(string(R.string.home_title))
    }

    @Test
    fun backOnRootExitsToLauncher() {
        // Root screen: back should finish the activity rather than popping.
        TestHelpers.waitForText(string(R.string.home_title))
        val scenario = activityRule.scenario
        androidx.test.espresso.Espresso.pressBack()
        TestHelpers.waitUntil { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
        assertEquals(
            androidx.lifecycle.Lifecycle.State.DESTROYED,
            scenario.state,
        )
    }
}