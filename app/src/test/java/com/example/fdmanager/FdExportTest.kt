package com.example.fdmanager

import com.example.fdmanager.data.model.CompoundingFrequency
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.PayoutFrequency
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.data.model.TenorUnit
import com.example.fdmanager.domain.ExportFormat
import com.example.fdmanager.domain.FdExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * #5 trust UX — export serialisation.
 *
 * The acceptance criterion is "export file opens correctly in a spreadsheet/reader". These tests
 * pin the two things that actually decide that: RFC 4180 quoting, and JSON escaping.
 */
class FdExportTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 25)

    private fun fd(
        id: String = "a",
        fdNumber: String = "NSB-78412",
        bank: String = "National Savings Bank (NSB)",
        amount: Double = 1_000_000.0,
        branch: String? = null,
        branchCode: String? = null
    ) = FixedDeposit(
        id = id,
        fdNumber = fdNumber,
        bank = bank,
        amount = amount,
        openedDate = today.minusMonths(6),
        durationMonths = 12,
        interestRate = 10.5,
        maturityDate = today.plusMonths(6),
        branch = branch,
        branchCode = branchCode
    )

    // ---- CSV ----

    @Test
    fun `csv starts with the fixed header and CRLF`() {
        val csv = FdExport.toCsv(listOf(fd()))
        val firstLine = csv.substringBefore("\r\n")
        assertEquals(FdExport.CSV_HEADER.joinToString(","), firstLine)
        assertTrue("must use CRLF per RFC 4180", csv.contains("\r\n"))
    }

    @Test
    fun `csv has one data row per deposit`() {
        val csv = FdExport.toCsv(listOf(fd(id = "a"), fd(id = "b"), fd(id = "c")))
        // header + 3 rows, each terminated by CRLF
        val rows = csv.split("\r\n").filter { it.isNotEmpty() }
        assertEquals(4, rows.size)
    }

    @Test
    fun `csv of an empty list is header only`() {
        val csv = FdExport.toCsv(emptyList())
        assertEquals(FdExport.CSV_HEADER.joinToString(",") + "\r\n", csv)
    }

    @Test
    fun `plain fields are not quoted`() {
        assertEquals("NSB-78412", FdExport.escapeCsv("NSB-78412"))
        assertEquals("1000", FdExport.escapeCsv("1000"))
    }

    @Test
    fun `comma forces quoting`() {
        assertEquals("\"Colombo, Fort\"", FdExport.escapeCsv("Colombo, Fort"))
    }

    @Test
    fun `double quote is doubled and the field is quoted`() {
        assertEquals("\"He said \"\"hello\"\"\"", FdExport.escapeCsv("He said \"hello\""))
    }

    @Test
    fun `newline forces quoting`() {
        assertEquals("\"line1\nline2\"", FdExport.escapeCsv("line1\nline2"))
        assertEquals("\"line1\rline2\"", FdExport.escapeCsv("line1\rline2"))
    }

    @Test
    fun `leading or trailing spaces are quoted so a reader cannot trim them`() {
        assertEquals("\" padded \"", FdExport.escapeCsv(" padded "))
    }

    @Test
    fun `branch containing a comma survives a round trip`() {
        val csv = FdExport.toCsv(listOf(fd(branch = "Colombo, Fort", branchCode = "001")))
        assertTrue("branch must be quoted", csv.contains("\"Colombo, Fort\""))
        val dataRow = csv.split("\r\n")[1]
        // A naive comma-split would produce more fields than the header declares.
        val parsed = parseCsvRow(dataRow)
        assertEquals(FdExport.CSV_HEADER.size, parsed.size)
    }

    @Test
    fun `whole-number amounts are written without a decimal point`() {
        val csv = FdExport.toCsv(listOf(fd(amount = 1_000_000.0)))
        val parsed = parseCsvRow(csv.split("\r\n")[1])
        val amountIndex = FdExport.CSV_HEADER.indexOf("amount")
        assertEquals("1000000", parsed[amountIndex])
        assertFalse("no scientific notation", parsed[amountIndex].contains("E"))
    }

    @Test
    fun `optional fields become empty strings, not the word null`() {
        val csv = FdExport.toCsv(listOf(fd(branch = null, branchCode = null)))
        val parsed = parseCsvRow(csv.split("\r\n")[1])
        assertEquals("", parsed[FdExport.CSV_HEADER.indexOf("branch")])
        assertEquals("", parsed[FdExport.CSV_HEADER.indexOf("branchCode")])
    }

    @Test
    fun `day-based tenor is exported`() {
        val f = fd().copy(tenorUnit = TenorUnit.DAYS, durationDays = 300)
        val parsed = parseCsvRow(FdExport.toCsv(listOf(f)).split("\r\n")[1])
        assertEquals("300", parsed[FdExport.CSV_HEADER.indexOf("durationDays")])
        assertEquals("DAYS", parsed[FdExport.CSV_HEADER.indexOf("tenorUnit")])
    }

    @Test
    fun `payout and renewal metadata are exported`() {
        val f = fd().copy(
            payoutFrequency = PayoutFrequency.MONTHLY,
            renewOption = RenewOption.CAPITALIZE,
            status = FdStatus.MATURED
        )
        val csv = FdExport.toCsv(listOf(f))
        val parsed = parseCsvRow(csv.split("\r\n")[1])
        assertEquals("MONTHLY", parsed[FdExport.CSV_HEADER.indexOf("payoutFrequency")])
        assertEquals("CAPITALIZE", parsed[FdExport.CSV_HEADER.indexOf("renewOption")])
        assertEquals("MATURED", parsed[FdExport.CSV_HEADER.indexOf("status")])
    }

    // ---- JSON ----

    @Test
    fun `json declares count and wraps deposits in a list`() {
        val json = FdExport.toJson(listOf(fd(id = "a"), fd(id = "b")))
        assertTrue(json.contains("\"count\": 2"))
        assertTrue(json.contains("\"deposits\": ["))
        assertTrue(json.trimEnd().endsWith("}"))
    }

    @Test
    fun `json of an empty list is still valid shape`() {
        val json = FdExport.toJson(emptyList())
        assertTrue(json.contains("\"count\": 0"))
        assertTrue(json.contains("\"deposits\": []"))
    }

    @Test
    fun `json escapes quotes backslashes and newlines`() {
        assertEquals("a\\\"b", FdExport.escapeJson("a\"b"))
        assertEquals("a\\\\b", FdExport.escapeJson("a\\b"))
        assertEquals("a\\nb", FdExport.escapeJson("a\nb"))
        assertEquals("a\\rb", FdExport.escapeJson("a\rb"))
        assertEquals("a\\tb", FdExport.escapeJson("a\tb"))
    }

    @Test
    fun `json omits nulls as literal null`() {
        val json = FdExport.toJson(listOf(fd(branch = null)))
        assertTrue(json.contains("\"branch\": null"))
    }

    @Test
    fun `json with a quote in the bank name stays parseable`() {
        val json = FdExport.toJson(listOf(fd(bank = "Bank \"X\"")))
        assertTrue(json.contains("\"bank\": \"Bank \\\"X\\\"\""))
    }

    // ---- file names ----

    @Test
    fun `suggested file name carries the date and extension`() {
        assertEquals("fd-manager-export-2026-10-25.csv",
            FdExport.suggestedFileName(ExportFormat.CSV, today))
        assertEquals("fd-manager-export-2026-10-25.json",
            FdExport.suggestedFileName(ExportFormat.JSON, today))
    }

    @Test
    fun `both formats are offered`() {
        assertEquals(2, ExportFormat.entries.size)
        assertEquals("csv", ExportFormat.CSV.extension)
        assertEquals("json", ExportFormat.JSON.extension)
    }

    // ---- Issue #6 — compounding column ----

    @Test
    fun `compounding is the last csv column and the header count is pinned`() {
        assertEquals("compounding", FdExport.CSV_HEADER.last())
        assertEquals(18, FdExport.CSV_HEADER.size)
        assertEquals(18, FdExport.CSV_HEADER.distinct().size)
    }

    @Test
    fun `csv and json both write the compounding frequency`() {
        val quarterly = fd().copy(compounding = CompoundingFrequency.QUARTERLY)

        val csv = FdExport.toCsv(listOf(quarterly)).trimEnd()
        val row = parseCsvRow(csv.split("\r\n")[1])
        assertEquals("QUARTERLY", row[FdExport.CSV_HEADER.indexOf("compounding")])

        val json = FdExport.toJson(listOf(quarterly))
        assertTrue(json.contains("\"compounding\": \"QUARTERLY\""))
    }

    @Test
    fun `a default fd exports as simple`() {
        val row = parseCsvRow(FdExport.toCsv(listOf(fd())).trimEnd().split("\r\n")[1])
        assertEquals("SIMPLE", row[FdExport.CSV_HEADER.indexOf("compounding")])
    }

    /** Minimal RFC 4180 reader — enough to prove a row splits back to the declared column count. */
    private fun parseCsvRow(row: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            when {
                inQuotes && c == '"' && i + 1 < row.length && row[i + 1] == '"' -> {
                    cur.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { out.add(cur.toString()); cur.clear() }
                else -> cur.append(c)
            }
            i++
        }
        out.add(cur.toString())
        return out
    }
}
