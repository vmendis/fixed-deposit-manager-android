package com.example.fdmanager

import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.Lkr
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FdMathTest {

    @Test
    fun `maturity date adds months and clamps end-of-month`() {
        assertEquals(LocalDate.of(2026, 2, 28), FdMath.maturityDate(LocalDate.of(2026, 1, 31), 1))
        assertEquals(LocalDate.of(2027, 9, 25), FdMath.maturityDate(LocalDate.of(2026, 9, 25), 12))
    }

    @Test
    fun `simple interest and maturity value`() {
        assertEquals(100_000.0, FdMath.interestEarned(1_000_000.0, 10.0, 12), 0.001)
        assertEquals(1_100_000.0, FdMath.maturityValue(1_000_000.0, 10.0, 12), 0.001)
        assertEquals(31_875.0, FdMath.interestEarned(750_000.0, 8.5, 6), 0.001)
    }

    @Test
    fun `days until maturity`() {
        val today = LocalDate.of(2026, 9, 25)
        assertEquals(4L, FdMath.daysUntil(LocalDate.of(2026, 9, 29), today))
        assertEquals(0L, FdMath.daysUntil(today, today))
        assertEquals(-6L, FdMath.daysUntil(LocalDate.of(2026, 9, 19), today))
    }

    @Test
    fun `countdown labels`() {
        assertEquals("matures in 4d", FdMath.countdownLabel(4L))
        assertEquals("matures tomorrow", FdMath.countdownLabel(1L))
        assertEquals("matures today", FdMath.countdownLabel(0L))
        assertEquals("matured 6d ago", FdMath.countdownLabel(-6L))

        assertEquals("in 4d", FdMath.countdownShort(4L))
        assertEquals("6d overdue", FdMath.countdownShort(-6L))
        assertEquals("today", FdMath.countdownShort(0L))
    }

    @Test
    fun `lkr formatting`() {
        assertEquals("Rs 1,500,000", Lkr.full(1_500_000.0))
        assertEquals("Rs 3.2 M", Lkr.compact(3_200_000.0))
        assertEquals("Rs 12.5 M", Lkr.compact(12_500_000.0))
        assertEquals("Rs 250 K", Lkr.compact(250_000.0))
        assertEquals("Rs 75,000", Lkr.compact(75_000.0))
    }

    @Test
    fun `hero wording spells Million with adaptive decimal and exact keeps cents`() {
        assertEquals("Rs 3.2 Million", Lkr.words(3_200_000.0))
        assertEquals("Rs 5 Million", Lkr.words(5_000_000.0))     // trailing .0 dropped
        assertEquals("Rs 12.5 Million", Lkr.words(12_500_000.0))
        assertEquals("Rs 750 K", Lkr.words(750_000.0))           // sub-million keeps K family
        assertEquals("Rs 0", Lkr.words(0.0))
        assertEquals("Rs 3,200,000.00", Lkr.exact(3_200_000.0))
        assertEquals("Rs 75,000.00", Lkr.exact(75_000.0))
    }

    @Test
    fun `rate formatting trims zeros`() {
        assertEquals("10", FdMath.formatRate(10.0))
        assertEquals("8.5", FdMath.formatRate(8.5))
        assertEquals("11.25", FdMath.formatRate(11.25))
    }

    // Issue #17 — day-based tenors
    @Test
    fun `maturity date plusDays for day-based tenor`() {
        val opened = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2026, 4, 11), FdMath.maturityDate(opened, 0, 100, com.example.fdmanager.data.model.TenorUnit.DAYS))
        assertEquals(LocalDate.of(2026, 10, 28), FdMath.maturityDate(opened, 0, 300, com.example.fdmanager.data.model.TenorUnit.DAYS))
        assertEquals(LocalDate.of(2026, 1, 31), FdMath.maturityDate(opened, 0, 30, com.example.fdmanager.data.model.TenorUnit.DAYS))
        assertEquals(LocalDate.of(2026, 1, 2), FdMath.maturityDate(opened, 0, 1, com.example.fdmanager.data.model.TenorUnit.DAYS))
    }

    @Test
    fun `interest days over 365`() {
        // 1,000,000 at 10% for 100 days = 1,000,000 * 0.10 * 100/365 = 27,397.26...
        assertEquals(27_397.26027, FdMath.interestEarned(1_000_000.0, 10.0, 0, 100, com.example.fdmanager.data.model.TenorUnit.DAYS), 0.01)
        // 300 days
        assertEquals(82_191.78082, FdMath.interestEarned(1_000_000.0, 10.0, 0, 300, com.example.fdmanager.data.model.TenorUnit.DAYS), 0.01)
        // 30 days
        assertEquals(6_164.38356, FdMath.interestEarned(750_000.0, 10.0, 0, 30, com.example.fdmanager.data.model.TenorUnit.DAYS), 0.01)
    }

    @Test
    fun `1-month first-class tenor`() {
        val opened = LocalDate.of(2026, 1, 31)
        // Jan 31 + 1 month = Feb 28 (clamped)
        assertEquals(LocalDate.of(2026, 2, 28), FdMath.maturityDate(opened, 1, null, com.example.fdmanager.data.model.TenorUnit.MONTHS))
        assertEquals(8_333.333, FdMath.interestEarned(1_000_000.0, 10.0, 1, null, com.example.fdmanager.data.model.TenorUnit.MONTHS), 0.01)
        assertEquals("1 month", FdMath.tenorLabel(1, null, com.example.fdmanager.data.model.TenorUnit.MONTHS))
        assertEquals("1 day", FdMath.tenorLabel(0, 1, com.example.fdmanager.data.model.TenorUnit.DAYS))
        assertEquals("100 days", FdMath.tenorLabel(0, 100, com.example.fdmanager.data.model.TenorUnit.DAYS))
        assertEquals("300 days", FdMath.tenorLabel(0, 300, com.example.fdmanager.data.model.TenorUnit.DAYS))
    }

    @Test
    fun `tenor label from FD`() {
        val fdMonths = com.example.fdmanager.data.model.FixedDeposit(
            id = "x", fdNumber = "X", bank = "NSB", amount = 1000.0,
            openedDate = LocalDate.of(2026,1,1), durationMonths = 3, interestRate = 10.0,
            maturityDate = LocalDate.of(2026,4,1),
            tenorUnit = com.example.fdmanager.data.model.TenorUnit.MONTHS
        )
        val fdDays = fdMonths.copy(durationMonths = 0, durationDays = 100, tenorUnit = com.example.fdmanager.data.model.TenorUnit.DAYS)
        assertEquals("3 months", FdMath.tenorLabel(fdMonths))
        assertEquals("100 days", FdMath.tenorLabel(fdDays))
    }
}
