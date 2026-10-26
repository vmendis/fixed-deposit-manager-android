package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import com.example.fdmanager.data.SettingsStore
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Issue #6 — compounding frequency on the form and on the detail screen.
 *
 * Acceptance: "Form exposes the option; preview and detail agree."
 *
 * Conventions copied from the classes that are already green, deliberately:
 *  - `createAndroidComposeRule` + the same `@Before` reset as `AddFdValidationTest`.
 *  - Chips are clicked with `performScrollTo()` first, because the compounding row sits in a
 *    `horizontalScroll` Row inside the form's `verticalScroll` Column — the same shape as the
 *    tenor preset rows that test already clicks successfully.
 *  - Assertions read the *preview value*, not the chip colour: `previewCompounding` is the text
 *    the user actually sees, so it proves the tap reached state rather than only the paint.
 */
@RunWith(AndroidJUnit4::class)
class CompoundingUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        SettingsStore.setHasSeenOnboarding(true)
        SettingsStore.clearLoadingErrorDebug()
        SettingsStore.setDemoMode(true)
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    private fun openAddForm() {
        rule.onNodeWithContentDescription("Add FD").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Compounding").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun fillAmountAndRate(amount: String, rate: String) {
        rule.onNodeWithText("Amount").performClick()
        rule.onNodeWithText("Amount").performTextInput(amount)
        rule.onNodeWithText("Interest rate").performClick()
        rule.onNodeWithText("Interest rate").performTextInput(rate)
        closeSoftKeyboard()
    }

    @Test
    fun everyCompoundingFrequencyIsOnTheForm() {
        openAddForm()
        listOf("SIMPLE", "MONTHLY", "QUARTERLY", "SEMI_ANNUAL", "ANNUAL").forEach { frequency ->
            rule.onNodeWithTag("compound_$frequency").assertExists()
        }
    }

    @Test
    fun aNewDepositStartsOnQuarterly() {
        openAddForm()
        rule.onNodeWithTag("compound_QUARTERLY").assertIsSelected()
    }

    @Test
    fun thePreviewFollowsTheSelectedFrequency() {
        openAddForm()
        fillAmountAndRate("1000000", "10")

        rule.waitUntil(5_000) {
            rule.onAllNodesWithTag("previewCompounding").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("previewCompounding").assertTextEquals("Quarterly")

        rule.onNodeWithTag("compound_SIMPLE").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("previewCompounding").assertTextEquals("Simple")

        rule.onNodeWithTag("compound_MONTHLY").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("previewCompounding").assertTextEquals("Monthly")
    }

    @Test
    fun aSavedDepositShowsItsCompoundingOnTheDetailScreen() {
        openAddForm()
        rule.onNodeWithText("FD number").performClick()
        rule.onNodeWithText("FD number").performTextInput("CMP-0001")
        fillAmountAndRate("500000", "9")

        rule.onNodeWithTag("compound_MONTHLY").performScrollTo().performClick()
        rule.onNodeWithText("Monthly payout").performScrollTo().performClick()
        rule.onNodeWithText("Save FD").performScrollTo().performClick()

        rule.onNodeWithText("FD Manager").assertIsDisplayed()

        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("CMP-0001").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithTag("detailCompounding").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("detailCompounding").assertTextEquals("Monthly")
    }

    @Test
    fun editingADepositKeepsTheFrequencyItWasSavedWith() {
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("NSB-78412").performClick()
        rule.onNodeWithContentDescription("Edit").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Compounding").fetchSemanticsNodes().isNotEmpty()
        }
        // NSB-78412 is seeded with QUARTERLY — editing must not reset it to the new-FD default
        // by accident, and must not silently fall back to SIMPLE either.
        rule.onNodeWithTag("compound_QUARTERLY").assertIsSelected()
    }

    @Test
    fun aDayBasedTenorCompoundsInThePreview() {
        openAddForm()
        fillAmountAndRate("1000000", "10")

        // Days 100 at 10% p.a. quarterly = one whole quarter plus a 8.33-day simple stub
        // → Rs 27,457.19, shown by Lkr.full as "Rs 27,457".
        rule.onNodeWithText("Days").performScrollTo().performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Rs 27,457", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            rule.onAllNodesWithText("Rs 27,457", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }
}
