package com.example.fdmanager

import com.example.fdmanager.data.SettingsState
import com.example.fdmanager.data.ThemeMode
import com.example.fdmanager.domain.DataWipe
import com.example.fdmanager.domain.WipeCategory
import com.example.fdmanager.domain.WipeSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #5 trust UX — delete-all-data.
 *
 * A wipe that clears the wrong thing, or keeps the wrong thing, is a trust defect. The rule these
 * tests pin: a category is only touched when its own box is ticked.
 */
class DataWipeTest {

    private fun populated() = SettingsState(
        themeMode = ThemeMode.DARK,
        notificationsEnabled = false,
        notifyLeadDays = 12,
        hasSeenOnboarding = true,
        isMockLoggedIn = true,
        mockEmail = "owner@example.com"
    )

    // ---- confirmation gate ----

    @Test
    fun `confirm phrase is the literal DELETE`() {
        assertEquals("DELETE", DataWipe.CONFIRM_PHRASE)
    }

    @Test
    fun `wrong phrase does not confirm`() {
        assertFalse(DataWipe.isConfirmed(""))
        assertFalse(DataWipe.isConfirmed("delete"))
        assertFalse(DataWipe.isConfirmed("DELETE ME"))
        assertFalse(DataWipe.isConfirmed("DELET"))
    }

    @Test
    fun `correct phrase confirms, surrounding spaces tolerated`() {
        assertTrue(DataWipe.isConfirmed("DELETE"))
        assertTrue(DataWipe.isConfirmed("  DELETE  "))
    }

    @Test
    fun `cannot wipe with nothing selected`() {
        assertFalse(DataWipe.canWipe(WipeSelection.NOTHING, "DELETE"))
    }

    @Test
    fun `cannot wipe with a selection but no phrase`() {
        assertFalse(DataWipe.canWipe(WipeSelection.EVERYTHING, ""))
        assertFalse(DataWipe.canWipe(WipeSelection.EVERYTHING, "DELET"))
    }

    @Test
    fun `can wipe with a selection and the exact phrase`() {
        assertTrue(DataWipe.canWipe(WipeSelection.EVERYTHING, "DELETE"))
        assertTrue(DataWipe.canWipe(WipeSelection.of(WipeCategory.FDS), "DELETE"))
    }

    // ---- deposits ----

    @Test
    fun `deposits are cleared only when the FDS box is ticked`() {
        assertTrue(WipeSelection.of(WipeCategory.FDS).clearsDeposits)
        assertFalse(WipeSelection.of(WipeCategory.SETTINGS).clearsDeposits)
        assertFalse(WipeSelection.of(WipeCategory.ACCOUNT, WipeCategory.ONBOARDING).clearsDeposits)
        assertFalse(WipeSelection.NOTHING.clearsDeposits)
    }

    // ---- settings isolation ----

    @Test
    fun `wiping preferences resets theme notifications and lead time`() {
        val out = DataWipe.applyTo(populated(), WipeSelection.of(WipeCategory.SETTINGS))
        val defaults = SettingsState()
        assertEquals(defaults.themeMode, out.themeMode)
        assertEquals(defaults.notificationsEnabled, out.notificationsEnabled)
        assertEquals(defaults.notifyLeadDays, out.notifyLeadDays)
    }

    @Test
    fun `wiping preferences leaves the session and onboarding flag alone`() {
        val out = DataWipe.applyTo(populated(), WipeSelection.of(WipeCategory.SETTINGS))
        assertTrue("still signed in", out.isMockLoggedIn)
        assertEquals("owner@example.com", out.mockEmail)
        assertTrue("onboarding still seen", out.hasSeenOnboarding)
    }

    @Test
    fun `wiping the session signs out and forgets the email only`() {
        val out = DataWipe.applyTo(populated(), WipeSelection.of(WipeCategory.ACCOUNT))
        assertFalse(out.isMockLoggedIn)
        assertNull(out.mockEmail)
        assertEquals("theme untouched", ThemeMode.DARK, out.themeMode)
        assertFalse("notifications untouched", out.notificationsEnabled)
        assertEquals(12, out.notifyLeadDays)
        assertTrue("onboarding untouched", out.hasSeenOnboarding)
    }

    @Test
    fun `wiping onboarding replays the welcome screen only`() {
        val out = DataWipe.applyTo(populated(), WipeSelection.of(WipeCategory.ONBOARDING))
        assertFalse(out.hasSeenOnboarding)
        assertTrue("session untouched", out.isMockLoggedIn)
        assertEquals(ThemeMode.DARK, out.themeMode)
    }

    @Test
    fun `wiping everything returns the defaults`() {
        val out = DataWipe.applyTo(populated(), WipeSelection.EVERYTHING)
        assertEquals(SettingsState(), out)
    }

    @Test
    fun `wiping nothing changes nothing`() {
        val before = populated()
        assertEquals(before, DataWipe.applyTo(before, WipeSelection.NOTHING))
    }

    @Test
    fun `unknown categories cannot leak in`() {
        val out = DataWipe.applyTo(populated(), WipeSelection.of(WipeCategory.FDS))
        assertEquals("settings must survive an FD-only wipe", populated(), out)
    }

    // ---- summary ----

    @Test
    fun `summary lists the selected categories in a stable order`() {
        assertEquals("Deposits", DataWipe.summary(WipeSelection.of(WipeCategory.FDS)))
        assertEquals(
            "Deposits, Preferences, Sign-in, Onboarding",
            DataWipe.summary(WipeSelection.EVERYTHING)
        )
    }

    @Test
    fun `summary of an empty selection says so`() {
        assertEquals("Nothing selected", DataWipe.summary(WipeSelection.NOTHING))
    }

    @Test
    fun `there are four wipe categories`() {
        assertEquals(4, WipeCategory.entries.size)
    }
}
