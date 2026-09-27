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

/** List filter chips and the sort menu. Ordering *logic* is covered by FdQueries unit tests. */
@RunWith(AndroidJUnit4::class)
class SortFilterTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun filterChipsNarrowTheList() {
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("NSB-78412").assertIsDisplayed()

        rule.onNodeWithText("Matured").performClick()
        rule.onNodeWithText("No matured FDs — try another filter.").assertIsDisplayed()
        rule.onNodeWithText("NSB-78412").assertDoesNotExist()

        rule.onNodeWithText("Active").performClick()
        rule.onNodeWithText("NSB-78412").assertIsDisplayed()

        rule.onNodeWithText("All").performClick()
        rule.onNodeWithText("NSB-78412").assertIsDisplayed()
    }

    @Test
    fun sortMenuAppliesSelection() {
        rule.openBank("National Savings Bank (NSB)")

        rule.onNodeWithContentDescription("Sort").performClick()
        rule.onNodeWithText("Amount (high first)").assertIsDisplayed()
        rule.onNodeWithText("Amount (high first)").performClick()

        // Menu dismissed, list intact
        rule.onNodeWithText("Amount (high first)").assertDoesNotExist()
        rule.onNodeWithText("NSB-78412").assertIsDisplayed()
    }
}
