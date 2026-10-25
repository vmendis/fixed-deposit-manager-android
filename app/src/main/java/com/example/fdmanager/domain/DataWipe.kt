package com.example.fdmanager.domain

import com.example.fdmanager.data.SettingsState

/**
 * #5 Trust UX — "Delete all data" with per-category control.
 *
 * Kept pure so the destructive logic is Tier1-testable: a wipe that clears the wrong thing, or
 * silently keeps the wrong thing, is a trust defect and must be provable without an emulator.
 */
enum class WipeCategory(val label: String, val detail: String) {
    FDS("Deposits", "Every FD, including the recycle bin and renewal history"),
    SETTINGS("Preferences", "Theme, notifications and reminder lead time"),
    ACCOUNT("Sign-in", "The mock session and remembered email"),
    ONBOARDING("Onboarding", "The 'seen the welcome screen' flag")
}

data class WipeSelection(val categories: Set<WipeCategory>) {

    val isEmpty: Boolean get() = categories.isEmpty()

    fun includes(category: WipeCategory): Boolean = category in categories

    /** The FDs are gone only when that box is ticked — never as a side effect of another box. */
    val clearsDeposits: Boolean get() = includes(WipeCategory.FDS)

    companion object {
        val NOTHING = WipeSelection(emptySet())
        val EVERYTHING = WipeSelection(WipeCategory.entries.toSet())
        fun of(vararg categories: WipeCategory) = WipeSelection(categories.toSet())
    }
}

object DataWipe {

    /** Text the owner must type before the wipe runs. Deliberately not a single keystroke. */
    const val CONFIRM_PHRASE: String = "DELETE"

    fun isConfirmed(typed: String): Boolean = typed.trim() == CONFIRM_PHRASE

    /** Button is enabled only with a non-empty selection AND the exact phrase. */
    fun canWipe(selection: WipeSelection, typed: String): Boolean =
        !selection.isEmpty && isConfirmed(typed)

    /**
     * Settings after the wipe. Categories that are not selected are carried over untouched —
     * this is the whole point of the per-category choice, so it is asserted directly in tests.
     */
    fun applyTo(current: SettingsState, selection: WipeSelection): SettingsState {
        var next = current
        if (selection.includes(WipeCategory.SETTINGS)) {
            next = next.copy(
                themeMode = SettingsState().themeMode,
                notificationsEnabled = SettingsState().notificationsEnabled,
                notifyLeadDays = SettingsState().notifyLeadDays
            )
        }
        if (selection.includes(WipeCategory.ACCOUNT)) {
            next = next.copy(isMockLoggedIn = false, mockEmail = null)
        }
        if (selection.includes(WipeCategory.ONBOARDING)) {
            next = next.copy(hasSeenOnboarding = false)
        }
        return next
    }

    /** One-line summary for the confirmation dialog. */
    fun summary(selection: WipeSelection): String {
        if (selection.isEmpty) return "Nothing selected"
        val names = WipeCategory.entries.filter { selection.includes(it) }.map { it.label }
        return names.joinToString(", ")
    }
}
