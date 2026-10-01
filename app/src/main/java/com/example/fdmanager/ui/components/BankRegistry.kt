package com.example.fdmanager.ui.components

import kotlin.math.pow

/**
 * Approximate identity for a Sri Lankan bank: the short code rendered on the
 * monogram tile plus an approximate brand hue (0xAARRGGBB).
 *
 * Colors are approximate brand hues chosen for glanceability — NOT official
 * brand assets. No logo artwork is stored or rendered anywhere in the app
 * (see private roadmap #18). [textArgb] overrides the automatic black/white
 * tile-text choice when a fixed color reads better.
 */
data class BankIdentity(
    val code: String,
    val colorArgb: Long,
    val textArgb: Long? = null
)

/**
 * Pure-Kotlin registry mapping bank names (as stored on [com.example.fdmanager.data.model.FixedDeposit])
 * to tile identities. Matching is case/punctuation-insensitive and works on
 * whole words, so users typing their own bank names still hit the registry.
 */
object BankRegistry {

    // ---- The 12 selectable banks (data.model.SRI_LANKAN_BANKS) -----------
    // Sample-data subset first, then the rest of the picker list.
    private val NSB = BankIdentity("NSB", 0xFF1B1B1B, textArgb = 0xFFF5A623) // black + gold emblem
    private val BOC = BankIdentity("BOC", 0xFFF2A900)                        // gold disc
    private val PB = BankIdentity("PB", 0xFFE53935)                          // red banner
    private val SAMP = BankIdentity("SAMP", 0xFFE87524)                      // orange octagon
    private val HNB = BankIdentity("HNB", 0xFF003B73)                        // navy
    private val COM = BankIdentity("COM", 0xFF1565A8)                        // blue
    private val NDB = BankIdentity("NDB", 0xFFC62828)                        // red slash
    private val DFCC = BankIdentity("DFCC", 0xFF7E2419)                      // brick
    private val SEY = BankIdentity("SEY", 0xFF9E1B2F)                        // wine
    private val NTB = BankIdentity("NTB", 0xFFAD1457)                        // magenta
    private val PAB = BankIdentity("PAB", 0xFFBF360C)                        // rust
    private val UB = BankIdentity("UB", 0xFF0288D1)                          // sky blue

    /** Neutral fallback tile for user-added / unknown banks. */
    private const val NEUTRAL_ARGB = 0xFF64748B
    private const val LIGHT_TEXT = 0xFFFFFFFFL
    private const val DARK_TEXT = 0xFF1C1B1FL

    private val STOPWORDS = setOf("of", "the", "and", "plc", "ltd", "limited", "co", "company")

    private val entries: List<Pair<List<String>, BankIdentity>> = listOf(
        listOf("national savings bank", "nsb") to NSB,
        listOf("bank of ceylon", "boc") to BOC,
        listOf("peoples bank", "pb") to PB,
        listOf("sampath bank", "sampath") to SAMP,
        listOf("hatton national bank", "hnb") to HNB,
        listOf("commercial bank of ceylon", "commercial bank", "combank") to COM,
        listOf("national development bank", "ndb bank", "ndb") to NDB,
        listOf("dfcc bank", "dfcc") to DFCC,
        listOf("seylan bank", "seylan") to SEY,
        listOf("nations trust bank", "nations trust", "ntb") to NTB,
        listOf("pan asia bank", "pan asia") to PAB,
        listOf("union bank", "ub") to UB
    ).map { (aliases, identity) -> aliases.map(::normalize) to identity }

    /** Find a bank by any of its known names/codes, or null if unknown. */
    fun find(bankName: String): BankIdentity? {
        val name = normalize(bankName)
        if (name.isBlank()) return null
        val padded = " $name "
        return entries.firstOrNull { (aliases, _) ->
            aliases.any { padded.contains(" $it ") }
        }?.second
    }

    /** Resolve a bank name, falling back to derived initials on a neutral tile. */
    fun resolve(bankName: String): BankIdentity = find(bankName) ?: fallback(bankName)

    /**
     * Unknown bank → neutral tile with initials: significant words up to three
     * ("Kandy Farmers Bank" → KFB), single word → first three letters.
     */
    fun fallback(bankName: String): BankIdentity {
        val words = normalize(bankName).split(" ").filter { it.isNotBlank() }
        val significant = words.filterNot { it in STOPWORDS }
        val src = significant.ifEmpty { words }
        val code = when {
            src.isEmpty() -> "?"
            src.size == 1 -> src[0].take(3).uppercase()
            else -> src.take(3).joinToString("") { it.take(1).uppercase() }
        }
        return BankIdentity(code, NEUTRAL_ARGB)
    }

    /** Tile text color: fixed override, else black/white by background luminance. */
    fun contentColorFor(identity: BankIdentity): Long = when {
        identity.textArgb != null -> identity.textArgb
        relativeLuminance(identity.colorArgb) > 0.179 -> DARK_TEXT // where black/white tie
        else -> LIGHT_TEXT
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** Lowercase, strip punctuation/apostrophes, collapse whitespace. */
    private fun normalize(raw: String): String = raw
        .lowercase()
        .replace("'", "")
        .replace("\u2019", "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}
