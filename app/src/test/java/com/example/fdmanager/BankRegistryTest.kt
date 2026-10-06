package com.example.fdmanager

import com.example.fdmanager.data.model.SRI_LANKAN_BANKS
import com.example.fdmanager.ui.components.BankRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankRegistryTest {

    private val expectedCodes = mapOf(
        "National Savings Bank (NSB)" to "NSB",
        "Bank of Ceylon (BOC)" to "BOC",
        "People's Bank" to "PB",
        "Sampath Bank" to "SAMP",
        "Hatton National Bank (HNB)" to "HNB",
        "Commercial Bank" to "COM",
        "NDB Bank" to "NDB",
        "DFCC Bank" to "DFCC",
        "Seylan Bank" to "SEY",
        "Nations Trust Bank" to "NTB",
        "Pan Asia Bank" to "PAB",
        "Union Bank" to "UB"
    )

    @Test
    fun `every selectable bank resolves to its expected code`() {
        SRI_LANKAN_BANKS.forEach { bank ->
            val identity = BankRegistry.find(bank)
            assertNotNull("no registry entry for '$bank'", identity)
            assertEquals(bank, expectedCodes.getValue(bank), identity!!.code)
        }
    }

    @Test
    fun `all twelve banks render distinct tiles (code AND color)`() {
        val identities = SRI_LANKAN_BANKS.map { BankRegistry.resolve(it) }
        assertEquals(12, identities.map { it.code }.distinct().size)
        assertEquals(12, identities.map { it.colorArgb }.distinct().size)
    }

    @Test
    fun `bare codes and short forms match`() {
        assertEquals("NSB", BankRegistry.find("nsb")?.code)
        assertEquals("BOC", BankRegistry.find("BOC")?.code)
        assertEquals("COM", BankRegistry.find("combank")?.code)
        assertEquals("HNB", BankRegistry.find("hnb")?.code)
    }

    @Test
    fun `apostrophes are ignored`() {
        assertEquals(
            BankRegistry.find("People's Bank"),
            BankRegistry.find("Peoples Bank")
        )
    }

    @Test
    fun `unknown banks fall back to derived initials on a neutral tile`() {
        // New InstitutionRegistry filters generic words (bank, finance, etc.) for initials — CBSL-only guardrail
        assertEquals("KF", BankRegistry.resolve("Kandy Farmers Bank").code)
        assertEquals("SER", BankRegistry.resolve("Serendib").code)
        assertEquals("?", BankRegistry.resolve("").code)
        assertNull(BankRegistry.find("Kandy Farmers Bank"))
    }

    @Test
    fun `tile text is legible against its background`() {
        // Bright backgrounds → dark text
        assertEquals(0xFF1C1B1FL, BankRegistry.contentColorFor(BankRegistry.resolve("Bank of Ceylon (BOC)")))
        assertEquals(0xFF1C1B1FL, BankRegistry.contentColorFor(BankRegistry.resolve("People's Bank")))
        // Dark backgrounds → light text
        assertEquals(0xFFFFFFFFL, BankRegistry.contentColorFor(BankRegistry.resolve("Hatton National Bank (HNB)")))
        assertEquals(0xFFFFFFFFL, BankRegistry.contentColorFor(BankRegistry.resolve("Commercial Bank")))
        // NSB keeps its gold-on-black emblem treatment
        assertEquals(0xFFF5A623L, BankRegistry.contentColorFor(BankRegistry.resolve("National Savings Bank (NSB)")))
    }

    @Test
    fun `sample-data bank colors do not collide`() {
        val sampleBanks = listOf(
            "National Savings Bank (NSB)", "Sampath Bank", "Hatton National Bank (HNB)",
            "Commercial Bank", "Bank of Ceylon (BOC)", "People's Bank", "DFCC Bank"
        )
        val colors = sampleBanks.map { BankRegistry.resolve(it).colorArgb }
        assertEquals(colors.size, colors.toSet().size)
        assertNotEquals(
            BankRegistry.resolve("Sampath Bank").code,
            BankRegistry.resolve("Bank of Ceylon (BOC)").code
        )
    }
}
