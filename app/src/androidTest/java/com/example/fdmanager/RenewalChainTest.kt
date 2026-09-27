package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

/** Spec renewal chain: renew ACTIVE child → new linked FD, parent marked Renewed. */
@RunWith(AndroidJUnit4::class)
class RenewalChainTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun renewCreatesLinkedChildAndMarksParentRenewed() {
        rule.openBank("Commercial Bank")
        rule.onNodeWithText("COM-33018-R1").performClick()

        // Detail: Renew button → "Renew FD?" dialog → confirm (dialog appends last)
        rule.onNodeWithText("Renew").performClick()
        rule.clickLastNodeWithText("Renew")

        // Lands on the new FD's detail (chain continues: -R2)
        rule.onNodeWithText("COM-33018-R2").performScrollTo().assertIsDisplayed()

        // Back → the renewed parent now wears the Renewed chip
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Renewed").assertIsDisplayed()

        // Back → the bank list holds the whole chain
        rule.onNodeWithContentDescription("Back").performClick()
        rule.swipeUntilText("COM-33018-R2")
        rule.onNodeWithText("COM-33018").assertIsDisplayed()
        rule.onNodeWithText("COM-33018-R1").assertIsDisplayed()
        rule.onNodeWithText("COM-33018-R2").assertIsDisplayed()
    }
}
