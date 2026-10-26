package com.example.fdmanager.data.model

import java.time.LocalDate

/** Mirrors the Firestore schema from the spec (users/{userId}/fds/{fdId}). */
enum class FdStatus(val label: String) {
    ACTIVE("Active"),
    MATURED("Matured"),
    RENEWED("Renewed")
}

enum class StatusFilter(val label: String) {
    ALL("All"), ACTIVE("Active"), MATURED("Matured"), RENEWED("Renewed")
}

enum class SortOption(val label: String) {
    MATURITY_ASC("Maturity date"),
    AMOUNT_DESC("Amount (high first)"),
    RATE_DESC("Interest rate")
}

/** Issue #25 — when the bank pays the FD's interest. Display metadata in v1 (no payout ledger). */
enum class PayoutFrequency(val label: String) {
    MONTHLY("Monthly payout"),
    AT_MATURITY("At maturity")
}

/** Issue #25 — stored renewal instruction: what happens to accrued interest when the FD renews. */
enum class RenewOption(val label: String) {
    CAPITALIZE("Add interest to capital"),
    PAYOUT("Withdraw interest")
}

/** Issue #17 — tenor unit: months (bank-style) or days (NBFI odd tenors 100/300-day, 1-month). */
enum class TenorUnit(val label: String) {
    MONTHS("Months"),
    DAYS("Days")
}

/**
 * Issue #6 — how often the bank adds accrued interest back to the principal.
 *
 * [periodsPerYear] is 0 for [SIMPLE], which is the "no compounding" case. Ordering matters:
 * the form renders the chips in declaration order and [FdMath] relies on the value increasing
 * with the number of periods.
 */
enum class CompoundingFrequency(val label: String, val periodsPerYear: Int) {
    SIMPLE("Simple", 0),
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    SEMI_ANNUAL("Half-yearly", 2),
    ANNUAL("Yearly", 1);

    val compounds: Boolean get() = periodsPerYear > 0
}

data class FixedDeposit(
    val id: String,
    val fdNumber: String,
    val bank: String,
    val amount: Double,
    val openedDate: LocalDate,
    val durationMonths: Int,
    val interestRate: Double,        // annual, percent
    val maturityDate: LocalDate,
    val branch: String? = null,
    val branchCode: String? = null,
    val payoutFrequency: PayoutFrequency = PayoutFrequency.AT_MATURITY, // issue #25 (form always asks)
    val renewOption: RenewOption = RenewOption.PAYOUT,                   // issue #25 (form pre-selects CAPITALIZE)
    val autoRenew: Boolean = false,
    val isActive: Boolean = true,
    val status: FdStatus = FdStatus.ACTIVE,
    val parentFdId: String? = null,  // links a renewal to the FD it replaced
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,   // soft delete (spec: never destroy data)
    // Issue #17 — day-based tenors
    val durationDays: Int? = null,                // used when tenorUnit=DAYS (1–999)
    val tenorUnit: TenorUnit = TenorUnit.MONTHS,  // MONTHS (default, backward compat) or DAYS
    /**
     * Issue #6 — compounding frequency. Defaults to [CompoundingFrequency.SIMPLE], which is
     * exactly the interest model every earlier round used: a record written before #6 keeps the
     * numbers it has always shown. The *form* defaults a new FD to QUARTERLY (Sri Lankan bank
     * practice) — see `AddEditFdScreen`.
     */
    val compounding: CompoundingFrequency = CompoundingFrequency.SIMPLE
)

/** Bank-wise aggregate shown on the home screen. */
data class BankSummary(
    val bank: String,
    val totalInvested: Double,   // ACTIVE + MATURED money (RENEWED parents excluded)
    val activeCount: Int,        // FDs with status ACTIVE
    val fdCount: Int,            // all visible FDs at this bank
    val nearestMaturity: LocalDate?
)

val SRI_LANKAN_BANKS = listOf(
    "National Savings Bank (NSB)",
    "Bank of Ceylon (BOC)",
    "People's Bank",
    "Sampath Bank",
    "Hatton National Bank (HNB)",
    "Commercial Bank",
    "NDB Bank",
    "DFCC Bank",
    "Seylan Bank",
    "Nations Trust Bank",
    "Pan Asia Bank",
    "Union Bank"
)

/** Full CBSL-regulated list — use InstitutionRegistry.allDisplayNames as source of truth in UI. Kept here for backward compat; prefer InstitutionRegistry. */
@Deprecated("Use InstitutionRegistry.allDisplayNames — CBSL-only ~61 institutions")
val SRI_LANKAN_INSTITUTIONS = SRI_LANKAN_BANKS
