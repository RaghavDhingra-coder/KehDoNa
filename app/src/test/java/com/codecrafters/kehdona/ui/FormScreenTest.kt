package com.codecrafters.kehdona.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class FormScreenTest {
    @get:Rule val composeRule = createComposeRule()

    /** Regression: content taller than the screen used to be squashed to zero height below the fold. */
    @Test
    fun contentTallerThanTheScreen_canBeScrolledTo_atFullSize() {
        composeRule.setContent {
            KehDoNaTheme {
                FormScreen {
                    repeat(20) { Text("Row $it", Modifier.height(80.dp)) }
                    Text("Last row")
                }
            }
        }

        composeRule.onNodeWithText("Last row").performScrollTo().assertIsDisplayed()
        val last = composeRule.onNodeWithText("Last row").fetchSemanticsNode()
        assertTrue("Last row has height ${last.size.height}", last.size.height > 0)
    }
}
