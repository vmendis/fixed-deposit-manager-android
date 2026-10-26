package com.example.fdmanager.domain

import com.example.fdmanager.data.model.CompoundingFrequency
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.TenorUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Pure financial calculations. Kept android-free so they run in plain JVM unit tests. */
object FdMath {

    fun maturityDate(opened: LocalDate, durationMonths: Int): LocalDate =
        opened.plusMonths(durationMonths.toLong())

    // Issue #17 — day-based tenors
    fun maturityDate(opened: LocalDate, durationMonths: Int, durationDays: Int?, tenorUnit: com.example.fdmanager.data.model.TenorUnit): LocalDate =
        if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS && durationDays != null) {
            opened.plusDays(durationDays.toLong())
        } else {
            opened.plusMonths(durationMonths.toLong())
        }

    fun maturityDate(opened: LocalDate, fd: com.example.fdmanager.data.model.FixedDeposit): LocalDate =
        maturityDate(opened, fd.durationMonths, fd.durationDays, fd.tenorUnit)

    /** Simple-interest estimate (typical for LKR FDs paid at maturity). */
    fun interestEarned(amount: Double, annualRatePercent: Double, durationMonths: Int): Double =
        amount * (annualRatePercent / 100.0) * (durationMonths / 12.0)

    // Issue #17 — day-based: days/365
    fun interestEarned(amount: Double, annualRatePercent: Double, durationMonths: Int, durationDays: Int?, tenorUnit: com.example.fdmanager.data.model.TenorUnit): Double =
        interestForYears(
            amount,
            annualRatePercent,
            yearsOf(durationMonths, durationDays, tenorUnit),
            CompoundingFrequency.SIMPLE
        )

    // ---- Issue #6 — compounding frequency ----

    /**
     * Fractional years carried by a tenor. Day tenors use the same 365-day convention as
     * `interestEarned`, so a day-based FD added in #17 keeps its old arithmetic.
     */
    fun yearsOf(durationMonths: Int, durationDays: Int?, tenorUnit: TenorUnit): Double =
        if (tenorUnit == TenorUnit.DAYS && durationDays != null) durationDays / 365.0
        else durationMonths / 12.0

    /**
     * Interest over [years] at [annualRatePercent], compounded [compounding] times a year.
     *
     * Interest is added at **whole** compounding periods only; whatever is left at the end of the
     * tenor earns simple interest, pro rata. That is how a bank actually pays:
     *  - a 6-month FD under *annual* compounding never reaches its first compounding date, so it
     *    earns simple interest;
     *  - a 6-month FD under *quarterly* compounding completes exactly two periods and compounds
     *    twice (Rs 50,625 on Rs 1,000,000 at 10%, not the Rs 51,906 a fractional-exponent formula
     *    would give);
     *  - a 100-day FD under *quarterly* compounding completes one period plus a simple stub.
     *
     * The textbook `(1 + r/n)^(n·t)` form is deliberately not used. For t &lt; 1/n it pays *less*
     * than simple interest, so switching a sub-year FD from Simple to Yearly would visibly drop the
     * estimate on screen — mathematically defensible, but it reads as a bug to the user.
     */
    fun interestForYears(
        amount: Double,
        annualRatePercent: Double,
        years: Double,
        compounding: CompoundingFrequency
    ): Double {
        val rate = annualRatePercent / 100.0
        val periodsPerYear = compounding.periodsPerYear
        if (periodsPerYear <= 0) return amount * rate * years
        if (years <= 0.0) return 0.0
        val periodYears = 1.0 / periodsPerYear
        val periods = years * periodsPerYear
        val wholePeriods = Math.floor(periods)
        val grown = amount * Math.pow(1.0 + rate * periodYears, wholePeriods)
        val stubYears = (periods - wholePeriods) * periodYears
        return grown * (1.0 + rate * stubYears) - amount
    }

    fun interestEarned(
        amount: Double,
        annualRatePercent: Double,
        durationMonths: Int,
        compounding: CompoundingFrequency
    ): Double =
        interestForYears(amount, annualRatePercent, durationMonths / 12.0, compounding)

    fun interestEarned(
        amount: Double,
        annualRatePercent: Double,
        durationMonths: Int,
        durationDays: Int?,
        tenorUnit: TenorUnit,
        compounding: CompoundingFrequency
    ): Double =
        interestForYears(amount, annualRatePercent, yearsOf(durationMonths, durationDays, tenorUnit), compounding)

    fun maturityValue(
        amount: Double,
        annualRatePercent: Double,
        durationMonths: Int,
        durationDays: Int?,
        tenorUnit: TenorUnit,
        compounding: CompoundingFrequency
    ): Double =
        amount + interestEarned(amount, annualRatePercent, durationMonths, durationDays, tenorUnit, compounding)

    fun interestEarned(fd: com.example.fdmanager.data.model.FixedDeposit): Double =
        interestForYears(
            fd.amount,
            fd.interestRate,
            yearsOf(fd.durationMonths, fd.durationDays, fd.tenorUnit),
            fd.compounding
        )

    fun maturityValue(amount: Double, annualRatePercent: Double, durationMonths: Int): Double =
        amount + interestEarned(amount, annualRatePercent, durationMonths)

    fun maturityValue(fd: com.example.fdmanager.data.model.FixedDeposit): Double =
        fd.amount + interestEarned(fd)

    /** Human-readable tenor label: "3 months" / "1 month" / "100 days" / "1 day" */
    fun tenorLabel(months: Int, days: Int?, unit: com.example.fdmanager.data.model.TenorUnit): String =
        if (unit == com.example.fdmanager.data.model.TenorUnit.DAYS && days != null) {
            if (days == 1) "1 day" else "$days days"
        } else {
            if (months == 1) "1 month" else "$months months"
        }

    fun tenorLabel(fd: com.example.fdmanager.data.model.FixedDeposit): String =
        tenorLabel(fd.durationMonths, fd.durationDays, fd.tenorUnit)

    fun daysUntil(maturity: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, maturity)

    fun countdownLabel(days: Long): String = when {
        days < 0 -> "matured ${-days}d ago"
        days == 0L -> "matures today"
        days == 1L -> "matures tomorrow"
        else -> "matures in ${days}d"
    }

    /** Short label for compact chips. */
    fun countdownShort(days: Long): String = when {
        days < 0 -> "${-days}d overdue"
        days == 0L -> "today"
        days == 1L -> "tomorrow"
        else -> "in ${days}d"
    }

    fun formatRate(rate: Double): String =
        if (rate == Math.floor(rate) && !rate.isInfinite()) rate.toLong().toString()
        else DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(rate)
}

/** LKR number formatting (locale-stable for tests). */
object Lkr {
    private val grouped: NumberFormat = NumberFormat.getInstance(Locale.US).apply {
        maximumFractionDigits = 0
        isGroupingUsed = true
    }

    fun full(amount: Double): String = "Rs " + grouped.format(amount)

    /**
     * Hero wording for prominent totals: "Rs 3.2 Million" at >= 1M (one decimal, trailing
     * `.0` dropped → "Rs 5 Million"); below that the compact K-style ("Rs 750 K") so the
     * Home card, the bank-row wording, and the hero stay one family. Issue #20.
     */
    fun words(amount: Double): String =
        if (Math.abs(amount) >= 1_000_000) {
            "Rs " + DecimalFormat("0.#", DecimalFormatSymbols(Locale.US))
                .format(amount / 1_000_000.0) + " Million"
        } else compact(amount)

    /** Exact secondary line: grouped with two decimals — "Rs 3,200,000.00". Issue #20. */
    fun exact(amount: Double): String =
        "Rs " + DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(amount)

    fun compact(amount: Double): String {
        val abs = Math.abs(amount)
        val short = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
        return when {
            abs >= 1_000_000 -> "Rs " + short.format(amount / 1_000_000.0) + " M"
            abs >= 100_000 -> "Rs " + short.format(amount / 1_000.0) + " K"
            else -> full(amount)
        }
    }
}

object Dates {
    private val UI: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
    fun format(date: LocalDate): String = date.format(UI)
}
