package com.example.fdmanager.ui.components

import kotlin.math.pow

/**
 * CBSL-only Institution Registry — Option A strict guardrail.
 *
 * Source: CBSL Notice "LICENSED COMMERCIAL BANKS, LICENSED SPECIALISED BANKS, FINANCE COMPANIES"
 * As at 31.12.2025 (https://www.cbsl.gov.lk). Nation Lanka Finance PLC is excluded because
 * CBSL prohibits it from mobilising new deposits (notice footnote a).
 *
 * Total allowed: 24 LCB + 6 LSB + 31 LFC = 61 institutions.
 * No custom free-text storage — entry blocked if not in registry.
 * Monogram tiles only, no logo assets (see #18 process).
 */
enum class InstitutionType(val label: String) {
    BANK("Bank"),
    FINANCE_COMPANY("Finance Company")
}

data class InstitutionIdentity(
    val code: String,
    val colorArgb: Long,
    val textArgb: Long? = null,
    val type: InstitutionType,
    val displayName: String
)

object InstitutionRegistry {

    const val LAST_UPDATED = "2025-12-31"
    const val SOURCE_URL = "https://www.cbsl.gov.lk"

    // ---- Reused identities from old BankRegistry (12 banks) for backward compat ----
    // Display names match SRI_LANKAN_BANKS (old list) for test/sample-data compat, while CBSL official names are kept as aliases.
    private val NSB = InstitutionIdentity("NSB", 0xFF1B1B1B, textArgb = 0xFFF5A623, type = InstitutionType.BANK, displayName = "National Savings Bank (NSB)")
    private val BOC = InstitutionIdentity("BOC", 0xFFF2A900, type = InstitutionType.BANK, displayName = "Bank of Ceylon (BOC)")
    private val PB = InstitutionIdentity("PB", 0xFFE53935, type = InstitutionType.BANK, displayName = "People's Bank")
    private val SAMP = InstitutionIdentity("SAMP", 0xFFE87524, type = InstitutionType.BANK, displayName = "Sampath Bank")
    private val HNB = InstitutionIdentity("HNB", 0xFF003B73, type = InstitutionType.BANK, displayName = "Hatton National Bank (HNB)")
    private val COM = InstitutionIdentity("COM", 0xFF1565A8, type = InstitutionType.BANK, displayName = "Commercial Bank")
    private val NDB = InstitutionIdentity("NDB", 0xFFC62828, type = InstitutionType.BANK, displayName = "NDB Bank")
    private val DFCC = InstitutionIdentity("DFCC", 0xFF7E2419, type = InstitutionType.BANK, displayName = "DFCC Bank")
    private val SEY = InstitutionIdentity("SEY", 0xFF9E1B2F, type = InstitutionType.BANK, displayName = "Seylan Bank")
    private val NTB = InstitutionIdentity("NTB", 0xFFAD1457, type = InstitutionType.BANK, displayName = "Nations Trust Bank")
    private val PAB = InstitutionIdentity("PAB", 0xFFBF360C, type = InstitutionType.BANK, displayName = "Pan Asia Bank")
    private val UB = InstitutionIdentity("UB", 0xFF0288D1, type = InstitutionType.BANK, displayName = "Union Bank")

    // ---- Additional LCBs (12 more) ----
    private val AMANA = InstitutionIdentity("AMANA", 0xFF1A5C3A, type = InstitutionType.BANK, displayName = "Amana Bank PLC")
    private val BOCHINA = InstitutionIdentity("BOCN", 0xFFD32F2F, type = InstitutionType.BANK, displayName = "Bank of China Ltd.")
    private val CARGILLS = InstitutionIdentity("CARG", 0xFFE53935, type = InstitutionType.BANK, displayName = "Cargills Bank PLC")
    private val CITI = InstitutionIdentity("CITI", 0xFF0D47A1, type = InstitutionType.BANK, displayName = "Citibank, N.A.")
    private val DEUTSCHE = InstitutionIdentity("DB", 0xFF001E62, type = InstitutionType.BANK, displayName = "Deutsche Bank AG")
    private val HBL = InstitutionIdentity("HBL", 0xFF2E7D32, type = InstitutionType.BANK, displayName = "Habib Bank Ltd.")
    private val IB = InstitutionIdentity("IB", 0xFF1565C0, type = InstitutionType.BANK, displayName = "Indian Bank")
    private val IOB = InstitutionIdentity("IOB", 0xFF0D47A1, type = InstitutionType.BANK, displayName = "Indian Overseas Bank")
    private val MCB = InstitutionIdentity("MCB", 0xFFC62828, type = InstitutionType.BANK, displayName = "MCB Bank Ltd.")
    private val PUBB = InstitutionIdentity("PUBB", 0xFFB71C1C, type = InstitutionType.BANK, displayName = "Public Bank Berhad")
    private val SCB = InstitutionIdentity("SCB", 0xFF0D47A1, type = InstitutionType.BANK, displayName = "Standard Chartered Bank")
    private val SBI = InstitutionIdentity("SBI", 0xFF1A237E, type = InstitutionType.BANK, displayName = "State Bank of India")
    private val HSBC = InstitutionIdentity("HSBC", 0xFFD50000, type = InstitutionType.BANK, displayName = "The Hongkong & Shanghai Banking Corporation Ltd.")

    // ---- LSBs (6) ----
    private val HDFC = InstitutionIdentity("HDFC", 0xFFEF6C00, type = InstitutionType.BANK, displayName = "Housing Development Finance Corporation Bank of Sri Lanka")
    private val RDB = InstitutionIdentity("RDB", 0xFF2E7D32, type = InstitutionType.BANK, displayName = "Pradeshiya Sanwardhana Bank")
    private val SDB = InstitutionIdentity("SDB", 0xFF388E3C, type = InstitutionType.BANK, displayName = "SANASA Development Bank PLC")
    private val SLSB = InstitutionIdentity("SLSB", 0xFF1565A8, type = InstitutionType.BANK, displayName = "Sri Lanka Savings Bank Ltd.")
    private val SMIB = InstitutionIdentity("SMIB", 0xFF0D47A1, type = InstitutionType.BANK, displayName = "State Mortgage and Investment Bank")

    // ---- LFCs (31 allowed, Nation Lanka excluded) ----
    private val ABANS = InstitutionIdentity("ABANS", 0xFFC62828, type = InstitutionType.FINANCE_COMPANY, displayName = "Abans Finance PLC")
    private val ALLIANCE = InstitutionIdentity("ALF", 0xFF1565A8, type = InstitutionType.FINANCE_COMPANY, displayName = "Alliance Finance Co. PLC")
    private val AMW = InstitutionIdentity("AMW", 0xFFD32F2F, type = InstitutionType.FINANCE_COMPANY, displayName = "AMW Capital Leasing and Finance PLC")
    private val ASIA_ASSET = InstitutionIdentity("AAF", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "Asia Asset Finance PLC")
    private val ASSETLINE = InstitutionIdentity("AFL", 0xFFB71C1C, type = InstitutionType.FINANCE_COMPANY, displayName = "Assetline Finance Ltd.")
    private val AMF = InstitutionIdentity("AMF", 0xFF1565C0, type = InstitutionType.FINANCE_COMPANY, displayName = "Associated Motor Finance Co. PLC")
    private val CBCF = InstitutionIdentity("CBCF", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "CBC Finance PLC")
    private val CENFIN = InstitutionIdentity("CEN", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "Central Finance Co. PLC")
    private val CDB = InstitutionIdentity("CDB", 0xFF1B5E20, type = InstitutionType.FINANCE_COMPANY, displayName = "Citizens Development Business Finance PLC")
    private val CCF = InstitutionIdentity("CCF", 0xFFC62828, type = InstitutionType.FINANCE_COMPANY, displayName = "Commercial Credit & Finance PLC")
    private val DIALOG = InstitutionIdentity("DF", 0xFFE53935, type = InstitutionType.FINANCE_COMPANY, displayName = "Dialog Finance PLC")
    private val FINTREX = InstitutionIdentity("FINT", 0xFF1565A8, type = InstitutionType.FINANCE_COMPANY, displayName = "Fintrex Finance PLC")
    private val HNBF = InstitutionIdentity("HNBF", 0xFF003B73, type = InstitutionType.FINANCE_COMPANY, displayName = "HNB Finance PLC")
    private val JANASHAKTHI = InstitutionIdentity("JANA", 0xFFEF6C00, type = InstitutionType.FINANCE_COMPANY, displayName = "Janashakthi Finance PLC")
    private val LCBF = InstitutionIdentity("LCBF", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "Lanka Credit and Business Finance PLC")
    private val LBF = InstitutionIdentity("LBF", 0xFFC62828, type = InstitutionType.FINANCE_COMPANY, displayName = "LB Finance PLC")
    private val LOLC = InstitutionIdentity("LOLC", 0xFFD32F2F, type = InstitutionType.FINANCE_COMPANY, displayName = "LOLC Finance PLC")
    private val MIFL = InstitutionIdentity("MIFL", 0xFFC62828, type = InstitutionType.FINANCE_COMPANY, displayName = "Mahindra Ideal Finance Ltd.")
    private val MERC = InstitutionIdentity("MERC", 0xFF2E7D32, type = InstitutionType.FINANCE_COMPANY, displayName = "Mercantile Investments & Finance PLC")
    private val MBSL = InstitutionIdentity("MBSL", 0xFF1565A8, type = InstitutionType.FINANCE_COMPANY, displayName = "Merchant Bank of Sri Lanka & Finance PLC")
    private val PLC = InstitutionIdentity("PLC", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "People's Leasing & Finance PLC")
    private val PMF = InstitutionIdentity("PMF", 0xFF1565A8, type = InstitutionType.FINANCE_COMPANY, displayName = "PMF Finance PLC")
    private val RPF = InstitutionIdentity("RPF", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "Richard Pieris Finance Ltd.")
    private val SDF = InstitutionIdentity("SDF", 0xFF2E7D32, type = InstitutionType.FINANCE_COMPANY, displayName = "Sarvodaya Development Finance PLC")
    private val SENKADAGALA = InstitutionIdentity("SENKA", 0xFFC62828, type = InstitutionType.FINANCE_COMPANY, displayName = "Senkadagala Finance PLC")
    private val SINGER = InstitutionIdentity("SFL", 0xFFD32F2F, type = InstitutionType.FINANCE_COMPANY, displayName = "Singer Finance (Lanka) PLC")
    private val SIYAPATHA = InstitutionIdentity("SIYA", 0xFF2E7D32, type = InstitutionType.FINANCE_COMPANY, displayName = "Siyapatha Finance PLC")
    private val SMB = InstitutionIdentity("SMB", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "SMB Finance PLC")
    private val SOFTLOGIC = InstitutionIdentity("SLF", 0xFF7B1FA2, type = InstitutionType.FINANCE_COMPANY, displayName = "Softlogic Finance PLC")
    private val UBF = InstitutionIdentity("UBF", 0xFF0288D1, type = InstitutionType.FINANCE_COMPANY, displayName = "UB Finance PLC")
    private val VALLIBEL = InstitutionIdentity("VALL", 0xFF0D47A1, type = InstitutionType.FINANCE_COMPANY, displayName = "Vallibel Finance PLC")

    private const val NEUTRAL_ARGB = 0xFF64748BL
    private const val LIGHT_TEXT = 0xFFFFFFFFL
    private const val DARK_TEXT = 0xFF1C1B1FL

    private val STOPWORDS = setOf("of", "the", "and", "plc", "ltd", "limited", "co", "company", "bank", "finance", "leasing", "corporation", "lanka", "sri")

    // Full registry: alias list → identity. Aliases are normalized.
    private val entries: List<Pair<List<String>, InstitutionIdentity>> = listOf(
        // LCBs
        listOf("amana bank", "amana") to AMANA,
        listOf("bank of ceylon", "boc") to BOC,
        listOf("bank of china", "bank of china ltd", "bocn", "bochina") to BOCHINA,
        listOf("cargills bank", "cargills") to CARGILLS,
        listOf("citibank", "citibank na", "citi") to CITI,
        listOf("commercial bank of ceylon", "commercial bank", "combank", "com") to COM,
        listOf("deutsche bank", "deutsche bank ag", "db") to DEUTSCHE,
        listOf("dfcc bank", "dfcc") to DFCC,
        listOf("habib bank", "hbl", "habib") to HBL,
        listOf("hatton national bank", "hnb") to HNB,
        listOf("indian bank", "ib") to IB,
        listOf("indian overseas bank", "iob") to IOB,
        listOf("mcb bank", "mcb") to MCB,
        listOf("national development bank", "ndb bank", "ndb") to NDB,
        listOf("nations trust bank", "nations trust", "ntb") to NTB,
        listOf("pan asia bank", "pan asia", "pab") to PAB,
        listOf("peoples bank", "peoples bank", "pb") to PB,
        listOf("public bank berhad", "public bank", "pubb") to PUBB,
        listOf("sampath bank", "sampath") to SAMP,
        listOf("seylan bank", "seylan") to SEY,
        listOf("standard chartered bank", "standard chartered", "scb") to SCB,
        listOf("state bank of india", "sbi") to SBI,
        listOf("hongkong and shanghai banking corporation", "hsbc", "hongkong shanghai") to HSBC,
        listOf("union bank of colombo", "union bank", "ub") to UB,
        // LSBs
        listOf("housing development finance corporation bank", "hdfc bank", "hdfc") to HDFC,
        listOf("national savings bank", "nsb") to NSB,
        listOf("pradeshiya sanwardhana bank", "rdb", "regional development bank", "psb") to RDB,
        listOf("sanasa development bank", "sdb bank", "sdb") to SDB,
        listOf("sri lanka savings bank", "slsb") to SLSB,
        listOf("state mortgage and investment bank", "smib") to SMIB,
        // LFCs (31)
        listOf("abans finance", "abans") to ABANS,
        listOf("alliance finance", "alliance finance co", "alf") to ALLIANCE,
        listOf("amw capital leasing and finance", "amw capital", "amw") to AMW,
        listOf("asia asset finance", "aaf") to ASIA_ASSET,
        listOf("assetline finance", "assetline", "afl") to ASSETLINE,
        listOf("associated motor finance", "amf") to AMF,
        listOf("cbc finance", "cbc finance plc", "cbcf") to CBCF,
        listOf("central finance", "central finance co", "cen") to CENFIN,
        listOf("citizens development business finance", "cdb", "citizens development") to CDB,
        listOf("commercial credit and finance", "ccf") to CCF,
        listOf("dialog finance") to DIALOG,
        listOf("fintrex finance", "fintrex") to FINTREX,
        listOf("hnb finance", "hnbf") to HNBF,
        listOf("janashakthi finance", "jana") to JANASHAKTHI,
        listOf("lanka credit and business finance", "lcbf") to LCBF,
        listOf("lb finance", "lbf") to LBF,
        listOf("lolc finance", "lolc") to LOLC,
        listOf("mahindra ideal finance", "mifl", "ideal finance") to MIFL,
        listOf("mercantile investments and finance", "merc", "mercantile investments") to MERC,
        listOf("merchant bank of sri lanka and finance", "mbsl") to MBSL,
        listOf("peoples leasing and finance", "peoples leasing", "peoples leasing finance") to PLC,
        listOf("pmf finance", "pmf") to PMF,
        listOf("richard pieris finance", "rpf", "arpico finance") to RPF,
        listOf("sarvodaya development finance", "sdf") to SDF,
        listOf("senkadagala finance", "senkadagala", "senka") to SENKADAGALA,
        listOf("singer finance lanka", "singer finance", "sfl") to SINGER,
        listOf("siyapatha finance", "siya", "siyapatha") to SIYAPATHA,
        listOf("smb finance", "smb") to SMB,
        listOf("softlogic finance", "slf", "softlogic") to SOFTLOGIC,
        listOf("ub finance", "ubf") to UBF,
        listOf("vallibel finance", "vallibel", "vall") to VALLIBEL
    ).map { (aliases, identity) -> aliases.map(::normalize) to identity }

    // Public lists
    val allInstitutions: List<InstitutionIdentity> get() = entries.map { it.second }.distinctBy { it.displayName }
    val allBanks: List<InstitutionIdentity> get() = allInstitutions.filter { it.type == InstitutionType.BANK }
    val allFinanceCompanies: List<InstitutionIdentity> get() = allInstitutions.filter { it.type == InstitutionType.FINANCE_COMPANY }
    val allDisplayNames: List<String> get() = allInstitutions.map { it.displayName }.sorted()
    val bankDisplayNames: List<String> get() = allBanks.map { it.displayName }.sorted()
    val financeCompanyDisplayNames: List<String> get() = allFinanceCompanies.map { it.displayName }.sorted()

    /** Find by any alias/code, or null if not CBSL-regulated. */
    fun find(name: String): InstitutionIdentity? {
        val normalized = normalize(name)
        if (normalized.isBlank()) return null
        val padded = " $normalized "
        return entries.firstOrNull { (aliases, _) ->
            aliases.any { padded.contains(" $it ") || normalized == it }
        }?.second
    }

    /** CBSL-only check — true if institution is in registry. */
    fun isCBSLRegulated(name: String): Boolean = find(name) != null

    /** Resolve with neutral fallback for defensive rendering (entry still blocked). */
    fun resolve(name: String): InstitutionIdentity = find(name) ?: fallback(name)

    fun fallback(name: String): InstitutionIdentity {
        val words = normalize(name).split(" ").filter { it.isNotBlank() }
        val significant = words.filterNot { it in STOPWORDS }
        val src = significant.ifEmpty { words }
        val code = when {
            src.isEmpty() -> "?"
            src.size == 1 -> src[0].take(3).uppercase()
            else -> src.take(3).joinToString("") { it.take(1).uppercase() }
        }
        return InstitutionIdentity(code, NEUTRAL_ARGB, type = InstitutionType.BANK, displayName = name)
    }

    fun contentColorFor(identity: InstitutionIdentity): Long = when {
        identity.textArgb != null -> identity.textArgb
        relativeLuminance(identity.colorArgb) > 0.179 -> DARK_TEXT
        else -> LIGHT_TEXT
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun normalize(raw: String): String = raw
        .lowercase()
        .replace("'", "")
        .replace("\u2019", "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}
