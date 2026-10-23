package com.codecrafters.kehdona.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.ui.screens.welcome.WelcomeScreen
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A common 411×914dp phone leaves roughly 866dp once status and navigation bars are
 * subtracted. The main call to action must be reachable there without scrolling.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h866dp")
class WelcomeLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun primaryActionVisibleWithoutScrolling_onStandardPhone() {
        composeRule.setContent { KehDoNaTheme { WelcomeScreen(onGetStarted = {}, onLogIn = {}) } }

        composeRule.onNodeWithText("Get started").assertIsDisplayed()
    }
}
