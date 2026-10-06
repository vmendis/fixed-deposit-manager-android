package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import org.junit.Assert.assertTrue
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
        // Issue #25 lengthened the form → Save sits below the fold; scrollTo per contract,
        // then scrollTo each error (fields sit above the scrolled position).
        rule.onNodeWithText("Save FD").performScrollTo().performClick()

        rule.onNodeWithText("Required — e.g. NSB-78412").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Enter an amount greater than 0").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Enter a rate between 0 and 30").performScrollTo().assertIsDisplayed()
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

        closeSoftKeyboard()
        // Issue #25: payout frequency has no default — pick one before saving.
        rule.onNodeWithText("Monthly payout").performScrollTo().performClick()
        rule.onNodeWithText("Save FD").performScrollTo().performClick()
        // Saved → popped back to Home
        rule.onNodeWithText("FD Manager").assertIsDisplayed()

        // Bank defaults to NSB; the new FD must be in that bank's list
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("TEST-0001").assertIsDisplayed()
        // Issue #17: card meta now shows tenor, not payout — e.g. "9.5% p.a. • 12 months • matures ..."
        assertTrue(
            rule.onAllNodesWithText("months", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun addFdWith1MonthTenor() {
        rule.onNodeWithContentDescription("Add FD").performClick()

        rule.onNodeWithText("FD number").performClick()
        rule.onNodeWithText("FD number").performTextInput("TEST-1M")
        rule.onNodeWithText("Amount").performClick()
        rule.onNodeWithText("Amount").performTextInput("50000")
        rule.onNodeWithText("Interest rate").performClick()
        rule.onNodeWithText("Interest rate").performTextInput("9")
        closeSoftKeyboard()

        // Tenor = Months (default) → pick 1m preset
        rule.onNodeWithText("1m").performScrollTo().performClick()
        rule.onNodeWithText("Monthly payout").performScrollTo().performClick()
        rule.onNodeWithText("Save FD").performScrollTo().performClick()

        rule.onNodeWithText("FD Manager").assertIsDisplayed()
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("TEST-1M").assertIsDisplayed()
        assertTrue(
            rule.onAllNodesWithText("1 month", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun addFdWith100DaysTenor() {
        rule.onNodeWithContentDescription("Add FD").performClick()

        rule.onNodeWithText("FD number").performClick()
        rule.onNodeWithText("FD number").performTextInput("TEST-100D")
        rule.onNodeWithText("Amount").performClick()
        rule.onNodeWithText("Amount").performTextInput("100000")
        rule.onNodeWithText("Interest rate").performClick()
        rule.onNodeWithText("Interest rate").performTextInput("10")
        closeSoftKeyboard()

        // Switch to Days → pick 100d preset (100/300-day NBFI specials)
        rule.onNodeWithText("Days").performScrollTo().performClick()
        rule.onNodeWithText("100d").performScrollTo().performClick()
        rule.onNodeWithText("Monthly payout").performScrollTo().performClick()
        rule.onNodeWithText("Save FD").performScrollTo().performClick()

        rule.onNodeWithText("FD Manager").assertIsDisplayed()
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("TEST-100D").assertIsDisplayed()
        assertTrue(
            rule.onAllNodesWithText("100 days", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun payoutFrequencyIsRequiredToSave() {
        rule.onNodeWithContentDescription("Add FD").performClick()

        rule.onNodeWithText("FD number").performClick()
        rule.onNodeWithText("FD number").performTextInput("TEST-0002")
        rule.onNodeWithText("Amount").performClick()
        rule.onNodeWithText("Amount").performTextInput("50000")
        rule.onNodeWithText("Interest rate").performClick()
        rule.onNodeWithText("Interest rate").performTextInput("8")
        closeSoftKeyboard()

        rule.onNodeWithText("Save FD").performScrollTo().performClick()

        // Still on the form with the issue-#25 validation message visible
        rule.onNodeWithText("Select when interest is paid").performScrollTo().assertIsDisplayed()
        // FD number label may be scrolled off after the scrollTo above → existence, not display
        rule.onNodeWithText("FD number").assertExists()
    }
}
