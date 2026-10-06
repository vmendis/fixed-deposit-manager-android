package com.example.fdmanager

import com.example.fdmanager.ui.components.InstitutionRegistry
import com.example.fdmanager.ui.components.InstitutionType
import org.junit.Assert.*
import org.junit.Test

class InstitutionRegistryTest {

    @Test
    fun `registry contains 61 CBSL-allowed institutions`() {
        // 24 LCB + 6 LSB + 31 LFC = 61 (Nation Lanka excluded)
        assertEquals(61, InstitutionRegistry.allInstitutions.size)
        assertEquals(30, InstitutionRegistry.allBanks.size) // 24 LCB + 6 LSB = 30 banks
        assertEquals(31, InstitutionRegistry.allFinanceCompanies.size)
    }

    @Test
    fun `all display names are distinct and sorted`() {
        val names = InstitutionRegistry.allDisplayNames
        assertEquals(names.size, names.distinct().size)
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `LCB and LSB are classified as BANK`() {
        assertEquals(InstitutionType.BANK, InstitutionRegistry.find("Bank of Ceylon")?.type)
        assertEquals(InstitutionType.BANK, InstitutionRegistry.find("Commercial Bank")?.type)
        assertEquals(InstitutionType.BANK, InstitutionRegistry.find("National Savings Bank")?.type)
        assertEquals(InstitutionType.BANK, InstitutionRegistry.find("HDFC Bank")?.type)
    }

    @Test
    fun `LFCs are classified as FINANCE_COMPANY`() {
        assertEquals(InstitutionType.FINANCE_COMPANY, InstitutionRegistry.find("LOLC Finance")?.type)
        assertEquals(InstitutionType.FINANCE_COMPANY, InstitutionRegistry.find("Peoples Leasing")?.type)
        assertEquals(InstitutionType.FINANCE_COMPANY, InstitutionRegistry.find("CDB")?.type)
        assertEquals(InstitutionType.FINANCE_COMPANY, InstitutionRegistry.find("Central Finance")?.type)
        assertEquals(InstitutionType.FINANCE_COMPANY, InstitutionRegistry.find("LB Finance")?.type)
    }

    @Test
    fun `CBSL-only check blocks non-regulated names`() {
        assertTrue(InstitutionRegistry.isCBSLRegulated("Bank of Ceylon"))
        assertTrue(InstitutionRegistry.isCBSLRegulated("LOLC Finance PLC"))
        assertTrue(InstitutionRegistry.isCBSLRegulated("Peoples Leasing & Finance PLC"))
        assertFalse(InstitutionRegistry.isCBSLRegulated("Kandy Farmers Bank"))
        assertFalse(InstitutionRegistry.isCBSLRegulated("Acme Investment"))
        assertFalse(InstitutionRegistry.isCBSLRegulated(""))
        assertFalse(InstitutionRegistry.isCBSLRegulated("   "))
    }

    @Test
    fun `Nation Lanka Finance is excluded per CBSL prohibition`() {
        // Nation Lanka Finance PLC is prohibited from mobilising new deposits — must NOT be allowed
        assertFalse(InstitutionRegistry.isCBSLRegulated("Nation Lanka Finance"))
        assertFalse(InstitutionRegistry.isCBSLRegulated("Nation Lanka Finance PLC"))
        assertNull(InstitutionRegistry.find("Nation Lanka Finance"))
        assertNull(InstitutionRegistry.find("Nation Lanka Finance PLC"))
    }

    @Test
    fun `alias matching works for common short forms`() {
        assertEquals("BOC", InstitutionRegistry.find("boc")?.code)
        assertEquals("COM", InstitutionRegistry.find("combank")?.code)
        assertEquals("LOLC", InstitutionRegistry.find("lolc")?.code)
        // plc alone is too generic (many names end with PLC) — must not match, use peoples leasing instead
        assertNull(InstitutionRegistry.find("plc"))
        assertEquals("PLC", InstitutionRegistry.find("peoples leasing")?.code)
        assertEquals("CDB", InstitutionRegistry.find("cdb")?.code)
        assertEquals("NSB", InstitutionRegistry.find("nsb")?.code)
    }

    @Test
    fun `apostrophes and punctuation are ignored`() {
        assertEquals(
            InstitutionRegistry.find("People's Bank"),
            InstitutionRegistry.find("Peoples Bank")
        )
        assertEquals(
            InstitutionRegistry.find("People's Leasing & Finance PLC"),
            InstitutionRegistry.find("Peoples Leasing Finance")
        )
    }

    @Test
    fun `fallback generates initials from significant words`() {
        // Generic words like bank, finance, lanka filtered
        assertEquals("KF", InstitutionRegistry.fallback("Kandy Farmers Bank").code)
        assertEquals("SER", InstitutionRegistry.fallback("Serendib").code)
        assertEquals("?", InstitutionRegistry.fallback("").code)
    }

    @Test
    fun `lastUpdated is set and source is CBSL`() {
        assertEquals("2025-12-31", InstitutionRegistry.LAST_UPDATED)
        assertTrue(InstitutionRegistry.SOURCE_URL.contains("cbsl.gov.lk"))
    }

    @Test
    fun `all tiles have distinct codes for glanceability`() {
        val identities = InstitutionRegistry.allInstitutions
        // Codes distinct — critical for monogram tiles
        assertEquals(identities.size, identities.map { it.code }.distinct().size)
        // Colors: brand hues are approximate, duplicates allowed for similar brands, but at least 20 distinct hues
        assertTrue(identities.map { it.colorArgb }.distinct().size >= 20)
    }
}
