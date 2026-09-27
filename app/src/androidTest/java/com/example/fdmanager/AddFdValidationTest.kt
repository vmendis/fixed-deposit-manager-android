package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Add/Edit form: validation errors and a successful save. */
@RunWith(AndroidJUnit4::class)
class AddFdValidationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun emptyFormShowsValidationErrors() {
        rule.onNodeWithContentDescription("Add FD").performClick()
        rule.onNodeWithText("Save FD").performClick()

        rule.onNodeWithText("Required — e.g. NSB-78412").assertIsDisplayed()
        rule.onNodeWithText("Enter an amount greater than 0").assertIsDisplayed()
        rule.onNodeWithText("Enter a rate between 0 and 30").assertIsDisplayed()
    }

    @Test
    fun validFormSavesNewFd() {
        rule.onNodeWithContentDescription("Add FD").performClick()

        rule.onNodeWithText("FD number").performClick()
        rule.onNodeWithText("FD number").performTextInput("TEST-0001")
        rule.onNodeWithText("Amount").performClick()
        rule.onNodeWithText("Amount").performTextInput("75000")
        rule.onNodeWithText("Interest rate").performClick()
        rule.onNodeWithText("Interest rate").performTextInput("9.5")

        rule.onNodeWithText("Save FD").performClick()
        // Saved → popped back to Home
        rule.onNodeWithText("FD Manager").assertIsDisplayed()

        // Bank defaults to NSB; the new FD must be in that bank's list
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("TEST-0001").assertIsDisplayed()
    }
}
