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
    val autoRenew: Boolean = false,
    val isActive: Boolean = true,
    val status: FdStatus = FdStatus.ACTIVE,
    val parentFdId: String? = null,  // links a renewal to the FD it replaced
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false   // soft delete (spec: never destroy data)
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
