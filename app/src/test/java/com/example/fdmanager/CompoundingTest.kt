package com.example.fdmanager

import com.example.fdmanager.data.model.CompoundingFrequency
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.TenorUnit
import com.example.fdmanager.domain.FdMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Issue #6 — compounding frequency.
 *
 * Every expected number here is the bank-style value, worked by hand from
 * "compound at whole periods, simple on the stub":
 *
 *   Rs 1,000,000 @ 10% p.a. for 12 months
 *     Simple        100,000.00
 *     Yearly        100,000.00   (one period, same as simple)
 *     Half-yearly   102,500.00   (1.05^2)
 *     Quarterly     103,812.89   (1.025^4)
 *     Monthly       104,713.07   (1 + 0.10/12)^12
 *
 * Those are the figures Sri Lankan banks publish for a 12-month FD, so the test pins the
 * model to the market rather than to itself.
 */
class CompoundingTest {

    private val principal = 1_000_000.0

    // ---- the frequency table itself ----

    @Test
    fun `periods per year are pinned`() {
        assertEquals(0, CompoundingFrequency.SIMPLE.periodsPerYear)
        assertEquals(12, CompoundingFrequency.MONTHLY.periodsPerYear)
        assertEquals(4, CompoundingFrequency.QUARTERLY.periodsPerYear)
        assertEquals(2, CompoundingFrequency.SEMI_ANNUAL.periodsPerYear)
        assertEquals(1, CompoundingFrequency.ANNUAL.periodsPerYear)
        assertEquals(5, CompoundingFrequency.entries.size)
        assertFalse(CompoundingFrequency.SIMPLE.compounds)
        assertTrue(CompoundingFrequency.QUARTERLY.compounds)
    }

    @Test
    fun `order of the enum is the order the form shows`() {
        assertEquals(
            listOf("SIMPLE", "MONTHLY", "QUARTERLY", "SEMI_ANNUAL", "ANNUAL"),
            CompoundingFrequency.entries.map { it.name }
        )
        assertEquals(
            listOf("Simple", "Monthly", "Quarterly", "Half-yearly", "Yearly"),
            CompoundingFrequency.entries.map { it.label }
        )
    }

    // ---- one year: the market's published figures ----

    @Test
    fun `one year at ten percent matches every bank published figure`() {
        assertEquals(100_000.0, interest(12, CompoundingFrequency.SIMPLE), 0.01)
        assertEquals(100_000.0, interest(12, CompoundingFrequency.ANNUAL), 0.01)
        assertEquals(102_500.0, interest(12, CompoundingFrequency.SEMI_ANNUAL), 0.01)
        assertEquals(103_812.89, interest(12, CompoundingFrequency.QUARTERLY), 0.01)
        assertEquals(104_713.07, interest(12, CompoundingFrequency.MONTHLY), 0.01)
    }

    @Test
    fun `two years at eleven and a quarter percent quarterly`() {
        assertEquals(
            124_219.54,
            FdMath.interestEarned(500_000.0, 11.25, 24, CompoundingFrequency.QUARTERLY),
            0.01
        )
    }

    @Test
    fun `six months at eight and a half percent quarterly compounds twice`() {
        assertEquals(
            32_213.67,
            FdMath.interestEarned(750_000.0, 8.5, 6, CompoundingFrequency.QUARTERLY),
            0.01
        )
        // Half-yearly and yearly complete one period and none respectively over six months,
        // so both fall back to simple interest — 31,875, not a fraction of a compounding period.
        assertEquals(31_875.0, FdMath.interestEarned(750_000.0, 8.5, 6, CompoundingFrequency.SEMI_ANNUAL), 0.01)
        assertEquals(31_875.0, FdMath.interestEarned(750_000.0, 8.5, 6, CompoundingFrequency.ANNUAL), 0.01)
    }

    // ---- the whole-period rule ----

    @Test
    fun `a tenor shorter than one compounding period earns simple interest`() {
        // 6 months = half a year; only a frequency of 2+ periods/year completes a period.
        assertEquals(50_000.0, interest(6, CompoundingFrequency.ANNUAL), 0.01)
        assertEquals(50_000.0, interest(6, CompoundingFrequency.SEMI_ANNUAL), 0.01)
        // ... and at 6 months quarterly completes exactly two.
        assertEquals(50_625.0, interest(6, CompoundingFrequency.QUARTERLY), 0.01)

        // 3 months = one quarter: quarterly completes exactly one period, so it equals simple.
        assertEquals(4_687.5, FdMath.interestEarned(250_000.0, 7.5, 3, CompoundingFrequency.QUARTERLY), 0.01)
        assertEquals(4_687.5, FdMath.interestEarned(250_000.0, 7.5, 3, CompoundingFrequency.SIMPLE), 0.01)

        // 1 month = one twelfth: monthly completes exactly one period, so it equals simple.
        assertEquals(8_333.33, interest(1, CompoundingFrequency.MONTHLY), 0.01)
        assertEquals(8_333.33, interest(1, CompoundingFrequency.SIMPLE), 0.01)
    }

    @Test
    fun `a part period left at maturity earns simple interest pro rata`() {
        // 5 months = 1 quarter plus 2 months of stub.
        // 1,000,000 × 1.025 × (1 + 0.10 × 2/12) = 1,042,083.33
        assertEquals(42_083.33, interest(5, CompoundingFrequency.QUARTERLY), 0.01)
        assertEquals(41_666.67, interest(5, CompoundingFrequency.SIMPLE), 0.01)
    }

    @Test
    fun `compounding never pays less than simple interest`() {
        for (months in listOf(1, 2, 3, 5, 6, 9, 11, 12, 18, 24, 36, 60)) {
            val simple = interest(months, CompoundingFrequency.SIMPLE)
            for (frequency in CompoundingFrequency.entries) {
                val compounded = interest(months, frequency)
                assertTrue(
                    "$frequency over $months months paid $compounded, below simple $simple",
                    compounded >= simple - 0.000001
                )
            }
        }
    }

    @Test
    fun `more frequent compounding pays at least as much over a full year`() {
        val values = listOf(
            CompoundingFrequency.SIMPLE,
            CompoundingFrequency.ANNUAL,
            CompoundingFrequency.SEMI_ANNUAL,
            CompoundingFrequency.QUARTERLY,
            CompoundingFrequency.MONTHLY
        ).map { interest(12, it) }
        for (i in 1 until values.size) {
            assertTrue("frequency $i paid ${values[i]} below ${values[i - 1]}", values[i] >= values[i - 1])
        }
    }

    // ---- edges ----

    @Test
    fun `zero rate earns nothing at every frequency`() {
        for (frequency in CompoundingFrequency.entries) {
            assertEquals(0.0, FdMath.interestEarned(principal, 0.0, 12, frequency), 0.000001)
        }
    }

    @Test
    fun `zero tenor earns nothing at every frequency`() {
        for (frequency in CompoundingFrequency.entries) {
            assertEquals(0.0, FdMath.interestForYears(principal, 10.0, 0.0, frequency), 0.000001)
        }
    }

    @Test
    fun `maturity value adds the compounded interest`() {
        assertEquals(
            1_103_812.89,
            FdMath.maturityValue(principal, 10.0, 12, null, TenorUnit.MONTHS, CompoundingFrequency.QUARTERLY),
            0.01
        )
    }

    // ---- day-based tenors (issue #17 tenor, issue #6 compounding) ----

    @Test
    fun `a hundred day tenor completes one quarter and a simple stub`() {
        assertEquals(
            27_457.19,
            FdMath.interestEarned(principal, 10.0, 0, 100, TenorUnit.DAYS, CompoundingFrequency.QUARTERLY),
            0.01
        )
        assertEquals(
            27_397.26,
            FdMath.interestEarned(principal, 10.0, 0, 100, TenorUnit.DAYS, CompoundingFrequency.SIMPLE),
            0.01
        )
    }

    @Test
    fun `a three hundred day tenor compounds three quarters`() {
        assertEquals(
            84_635.39,
            FdMath.interestEarned(principal, 10.0, 0, 300, TenorUnit.DAYS, CompoundingFrequency.QUARTERLY),
            0.01
        )
    }

    @Test
    fun `a full 365 day tenor matches one year of interest`() {
        assertEquals(100_000.0, FdMath.interestEarned(principal, 10.0, 0, 365, TenorUnit.DAYS, CompoundingFrequency.SIMPLE), 0.01)
        assertEquals(103_812.89, FdMath.interestEarned(principal, 10.0, 0, 365, TenorUnit.DAYS, CompoundingFrequency.QUARTERLY), 0.01)
    }

    // ---- the model and its back-compatibility ----

    @Test
    fun `an fd written before issue six keeps simple interest`() {
        val legacy = FixedDeposit(
            id = "x", fdNumber = "X-1", bank = "NSB", amount = principal,
            openedDate = LocalDate.of(2026, 1, 1), durationMonths = 12, interestRate = 10.0,
            maturityDate = LocalDate.of(2027, 1, 1)
        )
        assertEquals(CompoundingFrequency.SIMPLE, legacy.compounding)
        assertEquals(100_000.0, FdMath.interestEarned(legacy), 0.01)
    }

    @Test
    fun `an fd with quarterly compounding uses it`() {
        val fd = FixedDeposit(
            id = "y", fdNumber = "Y-1", bank = "NSB", amount = principal,
            openedDate = LocalDate.of(2026, 1, 1), durationMonths = 12, interestRate = 10.0,
            maturityDate = LocalDate.of(2027, 1, 1),
            compounding = CompoundingFrequency.QUARTERLY
        )
        assertEquals(103_812.89, FdMath.interestEarned(fd), 0.01)
        assertEquals(1_103_812.89, FdMath.maturityValue(fd), 0.01)
    }

    @Test
    fun `the pre issue six overloads still mean simple interest`() {
        assertEquals(100_000.0, FdMath.interestEarned(principal, 10.0, 12), 0.001)
        assertEquals(1_100_000.0, FdMath.maturityValue(principal, 10.0, 12), 0.001)
        assertEquals(
            27_397.26,
            FdMath.interestEarned(principal, 10.0, 0, 100, TenorUnit.DAYS),
            0.01
        )
    }

    // ---- helper ----

    private fun interest(months: Int, frequency: CompoundingFrequency): Double =
        FdMath.interestEarned(principal, 10.0, months, frequency)
}
