package com.example.fdmanager.domain

import com.example.fdmanager.data.model.BankSummary
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.SortOption
import com.example.fdmanager.data.model.StatusFilter
import java.time.LocalDate
import java.time.YearMonth

/** Pure list queries/aggregations over FD data. Android-free and unit-testable. */
object FdQueries {

    fun visible(fds: List<FixedDeposit>): List<FixedDeposit> = fds.filter { !it.isDeleted }

    fun bin(fds: List<FixedDeposit>): List<FixedDeposit> = fds.filter { it.isDeleted }

    fun byId(fds: List<FixedDeposit>, id: String): FixedDeposit? = fds.firstOrNull { it.id == id }

    /** Total still invested: ACTIVE + MATURED money; RENEWED parents are superseded by children. */
    fun totalInvested(fds: List<FixedDeposit>): Double =
        visible(fds).filter { it.status != FdStatus.RENEWED }.sumOf { it.amount }

    fun bankSummaries(fds: List<FixedDeposit>): List<BankSummary> =
        visible(fds).groupBy { it.bank }.map { (bank, list) ->
            BankSummary(
                bank = bank,
                totalInvested = list.filter { it.status != FdStatus.RENEWED }.sumOf { it.amount },
                activeCount = list.count { it.status == FdStatus.ACTIVE },
                fdCount = list.size,
                nearestMaturity = list.filter { it.status == FdStatus.ACTIVE }
                    .minOfOrNull { it.maturityDate }
            )
        }.sortedBy { it.bank }

    /** FDs maturing within [withinDays] days from [today] (inclusive), soonest first. */
    fun maturingSoon(
        fds: List<FixedDeposit>,
        today: LocalDate,
        withinDays: Int
    ): List<FixedDeposit> =
        visible(fds)
            .filter { it.status == FdStatus.ACTIVE }
            .filter { FdMath.daysUntil(it.maturityDate, today) in 0..withinDays }
            .sortedBy { it.maturityDate }

    fun forBank(
        fds: List<FixedDeposit>,
        bank: String,
        filter: StatusFilter,
        sort: SortOption
    ): List<FixedDeposit> {
        val base = visible(fds).filter { it.bank == bank }
        val filtered = when (filter) {
            StatusFilter.ALL -> base
            StatusFilter.ACTIVE -> base.filter { it.status == FdStatus.ACTIVE }
            StatusFilter.MATURED -> base.filter { it.status == FdStatus.MATURED }
            StatusFilter.RENEWED -> base.filter { it.status == FdStatus.RENEWED }
        }
        return when (sort) {
            SortOption.MATURITY_ASC -> filtered.sortedBy { it.maturityDate }
            SortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortOption.RATE_DESC -> filtered.sortedByDescending { it.interestRate }
        }
    }

    /**
     * Renewal lineage of an FD, oldest first, following parentFdId links.
     * Assumes a linear chain (an FD is renewed into at most one child).
     */
    fun renewalChain(fds: List<FixedDeposit>, id: String): List<FixedDeposit> {
        val all = visible(fds)
        var root = byId(all, id) ?: return emptyList()
        while (root.parentFdId != null) {
            root = byId(all, root.parentFdId!!) ?: break
        }
        val chain = mutableListOf(root)
        var current = root
        while (true) {
            val child = all.firstOrNull { it.parentFdId == current.id } ?: break
            chain.add(child)
            current = child
        }
        return chain
    }

    /** Anything (ACTIVE/MATURED/RENEWED) maturing on an exact calendar date. */
    fun maturitiesOn(fds: List<FixedDeposit>, date: LocalDate): List<FixedDeposit> =
        visible(fds).filter { it.maturityDate == date }.sortedBy { it.bank }

    fun maturitiesInMonth(
        fds: List<FixedDeposit>,
        month: YearMonth
    ): Map<LocalDate, List<FixedDeposit>> =
        visible(fds)
            .filter { YearMonth.from(it.maturityDate) == month }
            .groupBy { it.maturityDate }
}
