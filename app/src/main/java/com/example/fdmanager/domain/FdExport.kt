package com.example.fdmanager.domain

import com.example.fdmanager.data.model.FixedDeposit
import java.time.LocalDate

/**
 * #5 Trust UX — pure export serialisation. No Android dependencies so it is Tier1-testable.
 *
 * Acceptance for #5 is "export file opens correctly in a spreadsheet/reader". That is decided
 * entirely by quoting correctness, which is where CSV implementations usually break. Keeping
 * this pure lets the escaping be proven without an emulator.
 */
enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV — opens in Excel / Sheets", "csv", "text/csv"),
    JSON("JSON — structured, re-importable", "json", "application/json")
}

object FdExport {

    /** Column order is fixed and part of the contract — tests assert on it. */
    val CSV_HEADER: List<String> = listOf(
        "fdNumber", "bank", "amount", "interestRate", "openedDate", "durationMonths",
        "durationDays", "tenorUnit", "maturityDate", "payoutFrequency", "renewOption",
        "autoRenew", "status", "branch", "branchCode", "parentFdId", "isDeleted"
    )

    /** RFC 4180: CRLF line endings, quote when the field contains a delimiter, quote or newline. */
    fun toCsv(fds: List<FixedDeposit>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER.joinToString(",")).append("\r\n")
        for (fd in fds) {
            sb.append(csvRow(fd)).append("\r\n")
        }
        return sb.toString()
    }

    private fun csvRow(fd: FixedDeposit): String = listOf(
        fd.fdNumber,
        fd.bank,
        formatAmount(fd.amount),
        fd.interestRate.toString(),
        fd.openedDate.toString(),
        fd.durationMonths.toString(),
        fd.durationDays?.toString() ?: "",
        fd.tenorUnit.name,
        fd.maturityDate.toString(),
        fd.payoutFrequency.name,
        fd.renewOption.name,
        fd.autoRenew.toString(),
        fd.status.name,
        fd.branch ?: "",
        fd.branchCode ?: "",
        fd.parentFdId ?: "",
        fd.isDeleted.toString()
    ).joinToString(",") { escapeCsv(it) }

    /**
     * Quote a field when it contains a delimiter, a quote, or a line break; double any inner
     * quotes. Whitespace-only and leading/trailing spaces are also quoted so a spreadsheet
     * does not silently trim them.
     */
    fun escapeCsv(raw: String): String {
        val needsQuoting = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
            raw != raw.trim()
        if (!needsQuoting) return raw
        return '"' + raw.replace("\"", "\"\"") + '"'
    }

    /** Plain decimal — never scientific notation, which spreadsheets read as text. */
    private fun formatAmount(amount: Double): String {
        if (amount == amount.toLong().toDouble()) return amount.toLong().toString()
        return amount.toString()
    }

    fun toJson(fds: List<FixedDeposit>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"exportedAt\": \"").append(LocalDate.now()).append("\",\n")
        sb.append("  \"count\": ").append(fds.size).append(",\n")
        sb.append("  \"deposits\": [")
        if (fds.isNotEmpty()) {
            sb.append("\n")
            fds.forEachIndexed { i, fd ->
                sb.append("    ").append(jsonObject(fd))
                if (i < fds.lastIndex) sb.append(",")
                sb.append("\n")
            }
            sb.append("  ")
        }
        sb.append("]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun jsonObject(fd: FixedDeposit): String {
        val fields = linkedMapOf<String, String?>(
            "id" to fd.id,
            "fdNumber" to fd.fdNumber,
            "bank" to fd.bank,
            "amount" to formatAmount(fd.amount),
            "interestRate" to fd.interestRate.toString(),
            "openedDate" to fd.openedDate.toString(),
            "durationMonths" to fd.durationMonths.toString(),
            "durationDays" to fd.durationDays?.toString(),
            "tenorUnit" to fd.tenorUnit.name,
            "maturityDate" to fd.maturityDate.toString(),
            "payoutFrequency" to fd.payoutFrequency.name,
            "renewOption" to fd.renewOption.name,
            "autoRenew" to fd.autoRenew.toString(),
            "status" to fd.status.name,
            "branch" to fd.branch,
            "branchCode" to fd.branchCode,
            "parentFdId" to fd.parentFdId,
            "isDeleted" to fd.isDeleted.toString()
        )
        val body = fields.entries.joinToString(", ") { (k, v) ->
            if (v == null) "\"$k\": null" else "\"$k\": \"${escapeJson(v)}\""
        }
        return "{$body}"
    }

    fun escapeJson(raw: String): String {
        val sb = StringBuilder(raw.length + 8)
        for (c in raw) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Suggested file name — stable, sortable, no spaces. */
    fun suggestedFileName(format: ExportFormat, today: LocalDate = LocalDate.now()): String =
        "fd-manager-export-$today.${format.extension}"
}
