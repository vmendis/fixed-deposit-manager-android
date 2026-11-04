package com.example.fdmanager.data

import com.example.fdmanager.data.model.CompoundingFrequency
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.PayoutFrequency
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.data.model.TenorUnit
import java.time.LocalDate

/**
 * #10 — FixedDeposit ⇄ Firestore map, spec §3.2 field-for-field.
 *
 * PURE KOTLIN on purpose: no Firestore types in the signatures, so every rule here is
 * Tier1 unit-testable with zero Firebase. [FirestoreFdRepository] adds the two
 * server-timestamp fields (createdAt on create, updatedAt on every write) on top.
 *
 * Encoding decisions (locked):
 * - Dates (openedDate, maturityDate) → epoch-day Long ("number" per spec §3.2).
 * - Enums → name strings; reads fall back to each field's model default on bad/missing
 *   values, so a hand-edited or future-version document can never crash the app.
 * - `duration` (legacy months field) is written for spec §3.2 backward compat, ignored on read.
 * - `userId` is written by the repository (it owns the uid), not by the mapper.
 * - CLOUD_GATE = cloud-1000r1 — freshness marker for #10 Firestore-backed repository.
 */
object FdFirestoreMapper {

    const val CLOUD_GATE = "cloud-1000r1"

    /** Everything except createdAt/updatedAt/userId — the repository owns those. */
    fun toMap(fd: FixedDeposit): Map<String, Any?> = mapOf(
        "fdNumber" to fd.fdNumber,
        "bank" to fd.bank,
        "amount" to fd.amount,
        "openedDate" to fd.openedDate.toEpochDay(),
        "duration" to fd.durationMonths,            // legacy field, spec §3.2
        "durationMonths" to fd.durationMonths,
        "durationDays" to fd.durationDays,
        "tenorUnit" to fd.tenorUnit.name,
        "interestRate" to fd.interestRate,
        "compounding" to fd.compounding.name,
        "maturityDate" to fd.maturityDate.toEpochDay(),
        "branch" to fd.branch,
        "branchCode" to fd.branchCode,
        "payoutFrequency" to fd.payoutFrequency.name,
        "renewOption" to fd.renewOption.name,
        "autoRenew" to fd.autoRenew,
        "isActive" to fd.isActive,
        "status" to fd.status.name,
        "parentFdId" to fd.parentFdId,
        "isDeleted" to fd.isDeleted
    )

    /** Null = the document is unusable (no fdNumber/bank/amount) and is skipped, never crashed on. */
    fun fromMap(id: String, data: Map<String, Any?>): FixedDeposit? {
        val fdNumber = data["fdNumber"] as? String ?: return null
        val bank = data["bank"] as? String ?: return null
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        return FixedDeposit(
            id = id,
            fdNumber = fdNumber,
            bank = bank,
            amount = amount,
            openedDate = epochDay(data["openedDate"]) ?: LocalDate.now(),
            durationMonths = (data["durationMonths"] as? Number)?.toInt()
                ?: (data["duration"] as? Number)?.toInt() ?: 0,
            interestRate = (data["interestRate"] as? Number)?.toDouble() ?: 0.0,
            maturityDate = epochDay(data["maturityDate"]) ?: LocalDate.now(),
            branch = data["branch"] as? String,
            branchCode = data["branchCode"] as? String,
            payoutFrequency = enumOrDefault(data["payoutFrequency"], PayoutFrequency.AT_MATURITY),
            renewOption = enumOrDefault(data["renewOption"], RenewOption.PAYOUT),
            autoRenew = data["autoRenew"] as? Boolean ?: false,
            isActive = data["isActive"] as? Boolean ?: true,
            status = enumOrDefault(data["status"], FdStatus.ACTIVE),
            parentFdId = data["parentFdId"] as? String,
            createdAt = createdAtMillis(data["createdAt"]),
            isDeleted = data["isDeleted"] as? Boolean ?: false,
            durationDays = (data["durationDays"] as? Number)?.toInt(),
            tenorUnit = enumOrDefault(data["tenorUnit"], TenorUnit.MONTHS),
            compounding = enumOrDefault(data["compounding"], CompoundingFrequency.SIMPLE)
        )
    }

    private fun epochDay(v: Any?): LocalDate? =
        (v as? Number)?.let { runCatching { LocalDate.ofEpochDay(it.toLong()) }.getOrNull() }

    /**
     * createdAt arrives as com.google.firebase.Timestamp from the server, as null while a
     * freshly queued offline write hasn't been acknowledged, or as a plain number in tests.
     */
    private fun createdAtMillis(v: Any?): Long = when (v) {
        is Number -> v.toLong()
        is com.google.firebase.Timestamp -> v.toDate().time
        else -> System.currentTimeMillis()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(v: Any?, default: T): T =
        (v as? String)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
}
