package com.example.fdmanager

import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.data.model.SortOption
import com.example.fdmanager.data.model.StatusFilter
import com.example.fdmanager.domain.FdQueries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FdRepositoryTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)

    private fun seed() = listOf(
        FixedDeposit("a", "FD-A", "Bank X", 100_000.0, today.minusMonths(11), 12, 10.0, today.plusMonths(1)),
        FixedDeposit("b", "FD-B", "Bank X", 200_000.0, today.minusMonths(10), 12, 9.5, today.plusMonths(2)),
        FixedDeposit("c", "FD-C", "Bank X", 50_000.0, today.minusMonths(13), 12, 9.0, today.minusMonths(1),
            isActive = false, status = FdStatus.RENEWED),
        FixedDeposit("d", "FD-D", "Bank Y", 300_000.0, today.minusMonths(5), 6, 8.5, today.plusDays(4)),
        FixedDeposit("e", "FD-E", "Bank Y", 150_000.0, today.minusMonths(6), 3, 8.0, today.minusDays(2),
            status = FdStatus.MATURED),
        FixedDeposit("f", "FD-F", "Bank Y", 99_000.0, today.minusMonths(2), 12, 9.0, today.plusMonths(10),
            isDeleted = true)
    )

    private fun repo() = FdRepository(seed())

    @Test
    fun `visible excludes soft-deleted records`() {
        assertEquals(5, FdQueries.visible(seed()).size)
        assertEquals(listOf("f"), FdQueries.bin(seed()).map { it.id })
    }

    @Test
    fun `total invested skips renewed parents and deleted`() {
        assertEquals(750_000.0, FdQueries.totalInvested(seed()), 0.001) // a+b+d+e
    }

    @Test
    fun `bank summaries aggregate correctly`() {
        val summaries = FdQueries.bankSummaries(seed())
        assertEquals(listOf("Bank X", "Bank Y"), summaries.map { it.bank })
        val x = summaries[0]
        assertEquals(300_000.0, x.totalInvested, 0.001)
        assertEquals(2, x.activeCount)
        assertEquals(3, x.fdCount)
        assertEquals(today.plusMonths(1), x.nearestMaturity)
    }

    @Test
    fun `maturing soon respects the window and active status`() {
        val soon5 = FdQueries.maturingSoon(seed(), today, 5)
        assertEquals(listOf("d"), soon5.map { it.id })
        val soon40 = FdQueries.maturingSoon(seed(), today, 40)
        assertEquals(listOf("d", "a"), soon40.map { it.id })
        assertFalse(FdQueries.maturingSoon(seed(), today, 400).any { it.status != FdStatus.ACTIVE })
    }

    @Test
    fun `soft delete restore and permanent delete`() {
        val repo = repo()
        repo.softDelete("a")
        assertEquals(4, FdQueries.visible(repo.fds.value).size)
        assertEquals(setOf("f", "a"), FdQueries.bin(repo.fds.value).map { it.id }.toSet())

        repo.restore("a")
        assertEquals(5, FdQueries.visible(repo.fds.value).size)

        repo.deleteForever("f")
        assertNull(repo.get("f"))
        assertEquals(5, repo.fds.value.size)
    }

    @Test
    fun `renewal creates child linked by parent and marks old renewed`() {
        val repo = repo()
        val oldOpened = repo.get("a")!!.openedDate
        val oldMaturity = repo.get("a")!!.maturityDate

        val newId = repo.renew("a")

        val old = repo.get("a")!!
        assertEquals(FdStatus.RENEWED, old.status)
        assertFalse(old.isActive)

        val child = repo.get(newId)!!
        assertEquals("FD-A-R1", child.fdNumber)
        assertEquals(FdStatus.ACTIVE, child.status)
        assertTrue(child.isActive)
        assertEquals("a", child.parentFdId)
        assertEquals(oldMaturity, child.openedDate)
        assertEquals(oldMaturity.plusMonths(12), child.maturityDate)
        assertEquals(oldOpened.plusMonths(12), child.openedDate) // sanity on seed dates
    }

    @Test
    fun `renewal chain is ordered oldest first and increments number`() {
        val repo = repo()
        val r1 = repo.renew("a")
        val r2 = repo.renew(r1)

        assertEquals("FD-A-R1", repo.get(r1)!!.fdNumber)
        assertEquals("FD-A-R2", repo.get(r2)!!.fdNumber)
        assertEquals(FdStatus.RENEWED, repo.get(r1)!!.status)

        val chain = FdQueries.renewalChain(repo.fds.value, r2)
        assertEquals(listOf("a", r1, r2), chain.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cannot renew an already renewed fd`() {
        repo().renew("c")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cannot renew a deleted fd`() {
        repo().renew("f")
    }

    @Test
    fun `bank list filtering and sorting`() {
        val desc = FdQueries.forBank(seed(), "Bank X", StatusFilter.ALL, SortOption.AMOUNT_DESC)
        assertEquals(listOf("b", "a", "c"), desc.map { it.id })

        val matured = FdQueries.forBank(seed(), "Bank Y", StatusFilter.MATURED, SortOption.MATURITY_ASC)
        assertEquals(listOf("e"), matured.map { it.id })

        val asc = FdQueries.forBank(seed(), "Bank X", StatusFilter.ALL, SortOption.MATURITY_ASC)
        assertEquals("c", asc.first().id) // matured earliest → first
    }

    @Test
    fun `update replaces the record`() {
        val repo = repo()
        val edited = repo.get("b")!!.copy(amount = 250_000.0, interestRate = 9.75)
        repo.update(edited)
        assertEquals(250_000.0, repo.get("b")!!.amount, 0.001)
        assertEquals(9.75, repo.get("b")!!.interestRate, 0.001)
    }

    // ---- Issue #25: renew payout options + auto-renew sweep ----

    @Test
    fun `renew with CAPITALIZE adds accrued interest to principal`() {
        val repo = repo()
        val newId = repo.renew("d", RenewOption.CAPITALIZE)
        // FD-D: 300,000 @ 8.5% for 6 months → interest 12,750
        assertEquals(312_750.0, repo.get(newId)!!.amount, 0.001)
        assertEquals("FD-D-R1", repo.get(newId)!!.fdNumber)
    }

    @Test
    fun `renew with PAYOUT reopens with the original principal`() {
        val repo = repo()
        val newId = repo.renew("d", RenewOption.PAYOUT)
        assertEquals(300_000.0, repo.get(newId)!!.amount, 0.001)
    }

    @Test
    fun `renew without an explicit option uses the stored instruction`() {
        val capitalizing = seed().first { it.id == "d" }.copy(renewOption = RenewOption.CAPITALIZE)
        val repo = FdRepository(seed().map { if (it.id == "d") capitalizing else it })
        val newId = repo.renew("d")
        // 300,000 @ 8.5% × 6/12 → +12,750 (stored CAPITALIZE was applied)
        assertEquals(312_750.0, repo.get(newId)!!.amount, 0.001)
    }

    @Test
    fun `auto renew sweep renews only due flagged fds`() {
        val dueFlagged = seed().first { it.id == "e" }.copy(autoRenew = true)
        val repo = FdRepository(seed().map { if (it.id == "e") dueFlagged else it })

        val renewedIds = repo.autoRenewDue(today)

        assertEquals(listOf("e"), renewedIds)
        assertEquals(FdStatus.RENEWED, repo.get("e")!!.status)
        assertFalse(repo.get("e")!!.isActive)
        val child = repo.fds.value.first { it.parentFdId == "e" }
        assertEquals("FD-E-R1", child.fdNumber)
        assertEquals(FdStatus.ACTIVE, child.status)
        // e was MATURED @ 150,000, 8.0%, 3m, PAYOUT (model default) → principal unchanged
        assertEquals(150_000.0, child.amount, 0.001)
        // Untouched: active-not-due (a, b, d), matured-but-unflagged (none besides e), renewed (c)
        assertEquals(FdStatus.ACTIVE, repo.get("a")!!.status)
        assertEquals(FdStatus.ACTIVE, repo.get("b")!!.status)
        assertEquals(FdStatus.RENEWED, repo.get("c")!!.status)
    }

    // ---- Issue #17: day-based tenors ----
    @Test
    fun `renew preserves day-based tenor and uses days for interest`() {
        val dayFd = FixedDeposit(
            id = "day1", fdNumber = "LOLC-100", bank = "LOLC Finance", amount = 500_000.0,
            openedDate = today.minusDays(100), durationMonths = 0, interestRate = 12.0,
            maturityDate = today,
            durationDays = 100, tenorUnit = com.example.fdmanager.data.model.TenorUnit.DAYS,
            renewOption = RenewOption.CAPITALIZE, autoRenew = true
        )
        val repo = FdRepository(listOf(dayFd))
        val newId = repo.renew("day1")
        val child = repo.get(newId)!!
        assertEquals(com.example.fdmanager.data.model.TenorUnit.DAYS, child.tenorUnit)
        assertEquals(100, child.durationDays)
        assertEquals(today.plusDays(100), child.maturityDate)
        // interest = 500k * 12% * 100/365 = ~16,438.356
        assertEquals(516_438.356, child.amount, 0.1)
    }

    @Test
    fun `renew preserves 1-month tenor`() {
        val monthFd = FixedDeposit(
            id = "m1", fdNumber = "NSB-1M", bank = "NSB", amount = 100_000.0,
            openedDate = today.minusMonths(1), durationMonths = 1, interestRate = 10.0,
            maturityDate = today,
            tenorUnit = com.example.fdmanager.data.model.TenorUnit.MONTHS,
            renewOption = RenewOption.PAYOUT
        )
        val repo = FdRepository(listOf(monthFd))
        val newId = repo.renew("m1")
        val child = repo.get(newId)!!
        assertEquals(com.example.fdmanager.data.model.TenorUnit.MONTHS, child.tenorUnit)
        assertEquals(1, child.durationMonths)
        assertEquals(null, child.durationDays)
        assertEquals(today.plusMonths(1), child.maturityDate)
        assertEquals(100_000.0, child.amount, 0.001)
    }
}
