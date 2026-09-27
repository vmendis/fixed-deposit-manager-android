package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Roadmap #18 — bank monogram tiles render on summary cards and detail headers. */
@RunWith(AndroidJUnit4::class)
class BankMonogramUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun summaryCardsCarryMonogramTiles() {
        rule.onNodeWithText("By bank").performScrollTo()
        rule.swipeUntilTag("summary:National Savings Bank (NSB)")

        rule.onNode(
            hasTestTag("bankMonogram") and
                hasAnyAncestor(hasTestTag("summary:National Savings Bank (NSB)"))
        ).assertExists()
    }

    @Test
    fun nsbDetailShowsCodeTile() {
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("NSB-78412").performClick()
        // Exact text: only the monogram renders a bare "NSB" (the bank label is the full name)
        rule.onNodeWithText("NSB").assertIsDisplayed()
    }

    @Test
    fun bocDetailShowsCodeTile() {
        rule.openBank("Bank of Ceylon (BOC)")
        rule.onNodeWithText("BOC-90812").performClick()
        rule.onNodeWithText("BOC").assertIsDisplayed()
    }

    @Test
    fun monogramPresentInBankListAppBar() {
        rule.openBank("National Savings Bank (NSB)")
        rule.onNode(
            hasTestTag("bankMonogram") and
                hasAnyAncestor(hasTestTag("summary:National Savings Bank (NSB)")).not()
        ).assertExists()
    }
}
