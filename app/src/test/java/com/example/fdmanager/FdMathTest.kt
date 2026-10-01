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
}
