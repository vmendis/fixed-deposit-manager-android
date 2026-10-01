package com.example.fdmanager.domain

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

    /** Simple-interest estimate (typical for LKR FDs paid at maturity). */
    fun interestEarned(amount: Double, annualRatePercent: Double, durationMonths: Int): Double =
        amount * (annualRatePercent / 100.0) * (durationMonths / 12.0)

    fun maturityValue(amount: Double, annualRatePercent: Double, durationMonths: Int): Double =
        amount + interestEarned(amount, annualRatePercent, durationMonths)

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
