package com.example.fdmanager.data

import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.FdQueries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID

/**
 * In-memory stand-in for Firestore. Mirrors the spec's behaviours:
 * soft delete (isDeleted), renewal via parentFdId chains, active/status bookkeeping.
 */
class FdRepository(initial: List<FixedDeposit>) {

    private val _fds = MutableStateFlow(initial)
    val fds: StateFlow<List<FixedDeposit>> = _fds.asStateFlow()

    fun get(id: String): FixedDeposit? = FdQueries.byId(_fds.value, id)

    fun add(fd: FixedDeposit): String {
        val id = if (fd.id.isBlank()) "fd-" + UUID.randomUUID().toString() else fd.id
        _fds.value = _fds.value + fd.copy(id = id)
        return id
    }

    fun update(fd: FixedDeposit) {
        _fds.value = _fds.value.map { if (it.id == fd.id) fd else it }
    }

    /** Soft delete per spec — data is never destroyed here. */
    fun softDelete(id: String) {
        _fds.value = _fds.value.map { if (it.id == id) it.copy(isDeleted = true) else it }
    }

    fun restore(id: String) {
        _fds.value = _fds.value.map { if (it.id == id) it.copy(isDeleted = false) else it }
    }

    /** Bin-only hard delete (the one place data actually leaves the mock store). */
    fun deleteForever(id: String) {
        _fds.value = _fds.value.filterNot { it.id == id }
    }

    /**
     * Spec 4.4 — mark old FD RENEWED + inactive, create a child FD linked by parentFdId.
     * Child opens on the old FD's maturity date with the same terms.
     * Returns the new FD's id.
     */
    fun renew(id: String): String {
        val old = get(id)
        require(old != null && !old.isDeleted && old.status != FdStatus.RENEWED) {
            "Only an existing, non-renewed FD can be renewed"
        }
        val chainPos = FdQueries.renewalChain(_fds.value, id).size  // root=1 → child becomes -R1
        val baseNumber = old.fdNumber.replace(Regex("-R\\d+$"), "")
        val newId = "fd-" + UUID.randomUUID().toString()
        val child = old.copy(
            id = newId,
            fdNumber = "$baseNumber-R$chainPos",
            openedDate = old.maturityDate,
            maturityDate = FdMath.maturityDate(old.maturityDate, old.durationMonths),
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

    fun reset(sample: List<FixedDeposit>) {
        _fds.value = sample
    }

    companion object {
        @Volatile
        private var instance: FdRepository? = null

        fun get(): FdRepository = instance ?: synchronized(this) {
            instance ?: FdRepository(SampleData.seed(LocalDate.now())).also { instance = it }
        }
    }
}

/** Realistic Sri Lankan demo data, dated relative to "today" so the mock always looks alive. */
object SampleData {

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
            autoRenew = true,
            status = FdStatus.ACTIVE
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
            autoRenew = false,
            status = FdStatus.ACTIVE,
            isDeleted = true
        )
    )
}
