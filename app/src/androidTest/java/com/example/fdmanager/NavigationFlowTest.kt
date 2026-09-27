package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Home → bank list → FD detail → back, using real navigation affordances. */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun homeToListToDetailAndBack() {
        rule.onNodeWithText("Total invested").assertIsDisplayed()

        rule.openBank("Bank of Ceylon (BOC)")
        rule.onNodeWithText("Bank of Ceylon (BOC)").assertIsDisplayed()

        rule.onNodeWithText("BOC-90812").performClick()
        rule.onNodeWithText("FD number").assertIsDisplayed()
        rule.onNodeWithText("BOC-90812").assertIsDisplayed()

        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Bank of Ceylon (BOC)").assertIsDisplayed()

        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("FD Manager").assertIsDisplayed()
    }

    @Test
    fun homeEntryPointsExist() {
        rule.onNodeWithContentDescription("Add FD").assertIsDisplayed()
        rule.onNodeWithContentDescription("Recycle bin").assertIsDisplayed()
        rule.onNodeWithContentDescription("Maturing soon").assertIsDisplayed()
    }
}
