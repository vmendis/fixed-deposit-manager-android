package com.example.fdmanager.data

import com.example.fdmanager.data.model.CompoundingFrequency
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.PayoutFrequency
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.FdQueries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID

/**
 * #10 — the repository CONTRACT. Two implementations:
 * - [InMemoryFdRepository]: the original in-memory store (demo mode, signed-out sessions,
 *   ALL tests — zero Firebase traffic, exactly the pre-#10 behaviour).
 * - [FirestoreFdRepository]: users/{uid}/fds with offline persistence (signed-in sessions
 *   on builds where Firebase is configured).
 * [FdRepositoryProvider] owns the switch; UI code keeps calling [FdRepository.get].
 *
 * [renew] and [autoRenewDue] are default methods built ONLY on [get]/[add]/[update], so the
 * renewal rules (spec 4.4, issue #25) cannot diverge between backends.
 */
interface FdRepository {

    val fds: StateFlow<List<FixedDeposit>>

    fun get(id: String): FixedDeposit? = FdQueries.byId(fds.value, id)

    fun add(fd: FixedDeposit): String
    fun update(fd: FixedDeposit)

    /** Soft delete per spec — data is never destroyed here. */
    fun softDelete(id: String)
    fun restore(id: String)

    /** Bin-only hard delete (the one place data actually leaves the store). */
    fun deleteForever(id: String)

    /** Demo/reset helpers — ALWAYS local-only; the provider routes them to the in-memory store. */
    fun reset(sample: List<FixedDeposit>)
    fun setDemoMode(enabled: Boolean, sample: List<FixedDeposit> = SampleData.seed(LocalDate.now()))

    fun clearAll()

    /**
     * Spec 4.4 renew — default built on get/update/add; see class docs. CAPITALIZE adds accrued
     * interest to the child's principal, PAYOUT reopens with the original sum (issue #25).
     */
    fun renew(id: String, option: RenewOption? = null): String {
        val old = get(id)
        require(old != null && !old.isDeleted && old.status != FdStatus.RENEWED) {
            "Only an existing, non-renewed FD can be renewed"
        }
        val chosen = option ?: old.renewOption
        val childAmount = if (chosen == RenewOption.CAPITALIZE) {
            old.amount + FdMath.interestEarned(old)
        } else old.amount
        val chainPos = FdQueries.renewalChain(fds.value, id).size  // root=1 → child becomes -R1
        val baseNumber = old.fdNumber.replace(Regex("-R\\d+$"), "")
        val newId = "fd-" + UUID.randomUUID().toString()
        val child = old.copy(
            id = newId,
            amount = childAmount,
            fdNumber = "$baseNumber-R$chainPos",
            openedDate = old.maturityDate,
            maturityDate = FdMath.maturityDate(old.maturityDate, old),
            status = FdStatus.ACTIVE,
            isActive = true,
            isDeleted = false,
            parentFdId = old.id,
            createdAt = System.currentTimeMillis()
        )
        update(old.copy(status = FdStatus.RENEWED, isActive = false))
        add(child)
        return newId
    }

    /**
     * Issue #25 — session-start sweep: every due FD (maturity reached, auto-renew on,
     * not yet renewed/deleted) renews itself per its stored instruction. Returns the ids
     * of the parents that were renewed.
     */
    fun autoRenewDue(today: LocalDate = LocalDate.now()): List<String> {
        val due = fds.value.filter {
            !it.isDeleted && it.autoRenew && it.status != FdStatus.RENEWED &&
                !it.maturityDate.isAfter(today) &&
                (it.status == FdStatus.ACTIVE || it.status == FdStatus.MATURED)
        }
        due.forEach { renew(it.id, it.renewOption) }
        return due.map { it.id }  // parent ids that were renewed
    }

    companion object {
        // DEMO_GATE = demo-0100r9 — freshness marker for V25 #1 opt-in demo
        const val DEMO_GATE = "demo-0100r9"

        /** #10 — the provider-managed facade; callers never see the backend switch. */
        fun get(): FdRepository = FdRepositoryProvider.facade
    }
}

/**
 * The original in-memory store. Mirrors the spec's behaviours:
 * soft delete (isDeleted), renewal via parentFdId chains, active/status bookkeeping.
 */
class InMemoryFdRepository(initial: List<FixedDeposit>) : FdRepository {

    private val _fds = MutableStateFlow(initial)
    override val fds: StateFlow<List<FixedDeposit>> = _fds.asStateFlow()

    override fun add(fd: FixedDeposit): String {
        val id = if (fd.id.isBlank()) "fd-" + UUID.randomUUID().toString() else fd.id
        _fds.value = _fds.value + fd.copy(id = id)
        return id
    }

    override fun update(fd: FixedDeposit) {
        _fds.value = _fds.value.map { if (it.id == fd.id) fd else it }
    }

    override fun softDelete(id: String) {
        _fds.value = _fds.value.map { if (it.id == id) it.copy(isDeleted = true) else it }
    }

    override fun restore(id: String) {
        _fds.value = _fds.value.map { if (it.id == id) it.copy(isDeleted = false) else it }
    }

    override fun deleteForever(id: String) {
        _fds.value = _fds.value.filterNot { it.id == id }
    }

    /** Original atomic renew kept — single _fds assignment (interface default would be two). */
    override fun renew(id: String, option: RenewOption?): String {
        val old = get(id)
        require(old != null && !old.isDeleted && old.status != FdStatus.RENEWED) {
            "Only an existing, non-renewed FD can be renewed"
        }
        val chosen = option ?: old.renewOption
        val childAmount = if (chosen == RenewOption.CAPITALIZE) {
            old.amount + FdMath.interestEarned(old)
        } else old.amount
        val chainPos = FdQueries.renewalChain(_fds.value, id).size  // root=1 → child becomes -R1
        val baseNumber = old.fdNumber.replace(Regex("-R\\d+$"), "")
        val newId = "fd-" + UUID.randomUUID().toString()
        val child = old.copy(
            id = newId,
            amount = childAmount,
            fdNumber = "$baseNumber-R$chainPos",
            openedDate = old.maturityDate,
            maturityDate = FdMath.maturityDate(old.maturityDate, old),
            status = FdStatus.ACTIVE,
            isActive = true,
            isDeleted = false,
            parentFdId = old.id,
            createdAt = System.currentTimeMillis()
        )
        _fds.value = _fds.value.map {
            if (it.id == old.id) it.copy(status = FdStatus.RENEWED, isActive = false) else it
        } + child
        return newId
    }

    override fun reset(sample: List<FixedDeposit>) {
        _fds.value = sample
    }

    /**
     * Issue #1 — Demo mode opt-in: sample data becomes opt-in, not always-seeded.
     * Toggle on/off without corrupting user data:
     * - on: add sample FDs (fd1..fd8) if not already present, keep user FDs
     * - off: remove sample FDs by id, keep user FDs
     * Sample ids are fd1..fd8 from SampleData.seed
     */
    override fun setDemoMode(enabled: Boolean, sample: List<FixedDeposit>) {
        val sampleIds = sample.map { it.id }.toSet()
        val current = _fds.value
        _fds.value = if (enabled) {
            // Add sample FDs that are missing, keep user FDs
            val existingIds = current.map { it.id }.toSet()
            val toAdd = sample.filter { it.id !in existingIds }
            current + toAdd
        } else {
            // Remove sample FDs, keep user FDs (ids not in sample set)
            current.filterNot { it.id in sampleIds }
        }
    }

    override fun clearAll() {
        _fds.value = emptyList()
    }

}

/** Realistic Sri Lankan demo data, dated relative to "today" so the mock always looks alive. */
object SampleData {

    /** #10 — demo record ids. NEVER migrated to the cloud (owner decision: demo stays local). */
    val SAMPLE_IDS: Set<String> = setOf("fd1", "fd2", "fd3", "fd4", "fd5", "fd6", "fd7", "fd8")

    fun seed(today: LocalDate): List<FixedDeposit> = listOf(
        FixedDeposit(
            id = "fd1",
            fdNumber = "NSB-78412",
            bank = "National Savings Bank (NSB)",
            amount = 1_000_000.0,
            openedDate = today.minusMonths(12).plusDays(3),
            durationMonths = 12,
            interestRate = 10.0,
            maturityDate = today.plusDays(3),
            branch = "Colombo Main",
            branchCode = "001",
            payoutFrequency = PayoutFrequency.MONTHLY,
            renewOption = RenewOption.CAPITALIZE,
            autoRenew = true,
            status = FdStatus.ACTIVE,
            // Issue #6 — the demo's flagship FD compounds quarterly (Sri Lankan bank practice),
            // so the detail screen shows a compounded estimate without anyone having to add one.
            compounding = CompoundingFrequency.QUARTERLY
        ),
        FixedDeposit(
            id = "fd2",
            fdNumber = "SAMP-22391",
            bank = "Sampath Bank",
            amount = 750_000.0,
            openedDate = today.minusMonths(6).plusDays(12),
            durationMonths = 6,
            interestRate = 8.5,
            maturityDate = today.plusDays(12),
            branch = "Kandy",
            payoutFrequency = PayoutFrequency.AT_MATURITY,
            renewOption = RenewOption.PAYOUT,
            autoRenew = false,
            status = FdStatus.ACTIVE
        ),
        FixedDeposit(
            id = "fd3",
            fdNumber = "HNB-55104",
            bank = "Hatton National Bank (HNB)",
            amount = 500_000.0,
            openedDate = today.minusMonths(24).plusDays(48),
            durationMonths = 24,
            interestRate = 11.25,
            maturityDate = today.plusDays(48),
            branch = "Galle",
            payoutFrequency = PayoutFrequency.MONTHLY,
            renewOption = RenewOption.CAPITALIZE,
            autoRenew = false,
            status = FdStatus.ACTIVE
        ),
        // Renewal chain: fd4 matured last month and was renewed into fd5
        FixedDeposit(
            id = "fd4",
            fdNumber = "COM-33018",
            bank = "Commercial Bank",
            amount = 300_000.0,
            openedDate = today.minusMonths(13),
            durationMonths = 12,
            interestRate = 9.75,
            maturityDate = today.minusMonths(1),
            branch = "Colombo 03",
            payoutFrequency = PayoutFrequency.AT_MATURITY,
            renewOption = RenewOption.PAYOUT,
            autoRenew = true,
            isActive = false,
            status = FdStatus.RENEWED
        ),
        FixedDeposit(
            id = "fd5",
            fdNumber = "COM-33018-R1",
            bank = "Commercial Bank",
            amount = 300_000.0,
            openedDate = today.minusMonths(1),
            durationMonths = 12,
            interestRate = 10.25,
            maturityDate = today.plusMonths(11),
            branch = "Colombo 03",
            payoutFrequency = PayoutFrequency.AT_MATURITY,
            renewOption = RenewOption.PAYOUT,
            autoRenew = true,
            isActive = true,
            status = FdStatus.ACTIVE,
            parentFdId = "fd4"
        ),
        FixedDeposit(
            id = "fd6",
            fdNumber = "BOC-90812",
            bank = "Bank of Ceylon (BOC)",
            amount = 250_000.0,
            openedDate = today.minusMonths(3).minusDays(6),
            durationMonths = 3,
            interestRate = 7.5,
            maturityDate = today.minusDays(6),
            branch = "Nugegoda",
            payoutFrequency = PayoutFrequency.AT_MATURITY,
            renewOption = RenewOption.PAYOUT,
            autoRenew = false,
            status = FdStatus.MATURED
        ),
        FixedDeposit(
            id = "fd7",
            fdNumber = "PB-66420",
            bank = "People's Bank",
            amount = 400_000.0,
            openedDate = today.minusMonths(26),
            durationMonths = 36,
            interestRate = 12.0,
            maturityDate = today.plusMonths(10),
            branch = "Matara",
            payoutFrequency = PayoutFrequency.MONTHLY,
            renewOption = RenewOption.CAPITALIZE,
            autoRenew = false,
            status = FdStatus.ACTIVE
        ),
        // One soft-deleted record so the recycle bin has content
        FixedDeposit(
            id = "fd8",
            fdNumber = "DFCC-11223",
            bank = "DFCC Bank",
            amount = 150_000.0,
            openedDate = today.minusMonths(8),
            durationMonths = 12,
            interestRate = 9.5,
            maturityDate = today.plusMonths(4),
            branch = "Colombo 07",
            payoutFrequency = PayoutFrequency.AT_MATURITY,
            renewOption = RenewOption.PAYOUT,
            autoRenew = false,
            status = FdStatus.ACTIVE,
            isDeleted = true
        )
    )
}
