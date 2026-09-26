package com.example.fdmanager

import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
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
}
