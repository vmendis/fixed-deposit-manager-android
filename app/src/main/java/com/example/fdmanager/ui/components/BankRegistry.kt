package com.example.fdmanager.ui.components

/**
 * Backward-compat shim — old 12-bank registry now delegates to CBSL-only InstitutionRegistry.
 * New code should use InstitutionRegistry directly.
 * Kept so existing tests and BankMonogram continue to work.
 */
typealias BankIdentity = InstitutionIdentity

object BankRegistry {

    fun find(bankName: String): BankIdentity? = InstitutionRegistry.find(bankName)

    fun resolve(bankName: String): BankIdentity = InstitutionRegistry.resolve(bankName)

    fun fallback(bankName: String): BankIdentity = InstitutionRegistry.fallback(bankName)

    fun contentColorFor(identity: BankIdentity): Long = InstitutionRegistry.contentColorFor(identity)
}
