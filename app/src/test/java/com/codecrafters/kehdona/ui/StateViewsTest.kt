package com.codecrafters.kehdona.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.ui.components.EmptyState
import com.codecrafters.kehdona.ui.components.ErrorState
import com.codecrafters.kehdona.ui.components.LoadingState
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateViewsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun loadingState_showsMessage() {
        composeRule.setContent { KehDoNaTheme { LoadingState(message = "Loading your matches…") } }

        composeRule.onNodeWithText("Loading your matches…").assertIsDisplayed()
    }

    @Test
    fun errorState_retryInvokesCallback() {
        var retries = 0
        composeRule.setContent {
            KehDoNaTheme { ErrorState(title = "Oops", message = "No connection.", onRetry = { retries++ }) }
        }

        composeRule.onNodeWithText("No connection.").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun errorState_withoutRetry_hidesButton() {
        composeRule.setContent { KehDoNaTheme { ErrorState(title = "Oops", message = "Broken.") } }

        composeRule.onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun emptyState_showsCopyAndAction() {
        composeRule.setContent {
            KehDoNaTheme {
                EmptyState(
                    icon = Icons.Filled.Info,
                    title = "No confessions yet",
                    message = "Be the first to say something.",
                    action = { Text("Write one") },
                )
            }
        }

        composeRule.onNodeWithText("No confessions yet").assertIsDisplayed()
        composeRule.onNodeWithText("Be the first to say something.").assertIsDisplayed()
        composeRule.onNodeWithText("Write one").assertIsDisplayed()
    }
}
