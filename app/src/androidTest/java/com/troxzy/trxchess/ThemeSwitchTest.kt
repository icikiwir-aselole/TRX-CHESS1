package com.troxzy.trxchess

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Theme switching from settings. The design system drives colors; changing
 * the theme rebuilds the current screen (state stays in the activity-scoped
 * ViewModels) — the app must stay stable and the settings screen must remain.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ThemeSwitchTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun string(resId: Int): String = ctx.getString(resId)

    private fun openSettings() {
        TestHelpers.waitForView(withContentDescription(string(R.string.home_settings)))
        onView(withContentDescription(string(R.string.home_settings)))
            .perform(scrollTo(), click())
        TestHelpers.waitForText(string(R.string.settings_title))
    }

    @Test
    fun switchingToLightKeepsSettingsStable() {
        openSettings()
        onView(withContentDescription(string(R.string.settings_appearance_theme)))
            .perform(scrollTo(), click())

        onView(withText(string(R.string.settings_appearance_theme_light)))
            .inRoot(isDialog())
            .perform(click())

        // Theme change rebuilds the screen; settings must reappear.
        TestHelpers.waitForText(string(R.string.settings_title))
        TestHelpers.waitForView(withContentDescription(string(R.string.settings_appearance_theme)))
    }

    @Test
    fun switchingToSystemKeepsSettingsStable() {
        openSettings()
        onView(withContentDescription(string(R.string.settings_appearance_theme)))
            .perform(scrollTo(), click())

        onView(withText(string(R.string.settings_appearance_theme_system)))
            .inRoot(isDialog())
            .perform(click())

        TestHelpers.waitForText(string(R.string.settings_title))
    }

    @Test
    fun cancelingThemeDialogChangesNothing() {
        openSettings()
        onView(withContentDescription(string(R.string.settings_appearance_theme)))
            .perform(scrollTo(), click())

        onView(withText(string(R.string.nav_back)))
            .inRoot(isDialog())
            .perform(click())

        TestHelpers.waitForText(string(R.string.settings_title))
    }
}