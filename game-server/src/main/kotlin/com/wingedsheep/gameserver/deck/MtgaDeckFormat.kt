package com.wingedsheep.gameserver.deck

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.Deck

/**
 * Parser/serializer for the MTG Arena plain-text decklist format.
 *
 * The Arena export uses section headers — `Deck`, `Sideboard`, `Companion`, `Commander` — with
 * count-prefixed card lines under each. Example:
 *
 * ```
 * Deck
 * 1 Sol Ring
 * 1 Arcane Signet
 * ...
 *
 * Commander
 * 1 Yuriko, the Tiger's Shadow
 * ```
 *
 * Lines may also carry a trailing `(SET) <collector-number>` annotation that MTGA emits — the
 * parser strips that so paste-imports from Arena/Moxfield/Archidekt all round-trip cleanly.
 *
 * **Commander fallback:** when a commander-shaped format ([DeckFormat.isCommanderShape]) is
 * imported without an explicit `Commander` section, the first card listed is promoted to the
 * commander slot. This mirrors what Moxfield/EDHREC do on import — explicit beats positional,
 * but positional still works for casual lists. The fallback is format-gated so a 60-card
 * Standard list never has its first card silently elevated.
 *
 * Sideboard and Companion sections are recognised but currently dropped — there's no place to
 * put them in [Deck] yet.
 *
 * Not the import path players use: pasted lists are parsed client-side by
 * `web-client/src/components/deckbuilder/parseArenaDeck.ts`, which also reads the Archidekt /
 * TappedOut / deckstats markers and infers an MTGO-style sideboard commander. Nothing in
 * `main` calls this object; if a server endpoint ever does, port those rules here first.
 */
object MtgaDeckFormat {

    private val LINE = Regex("""^\s*(\d+)\s+(.+?)\s*$""")
    private val SET_SUFFIX = Regex("""\s*\([^)]*\)\s+\S+\s*$""")

    /**
     * Parse [text] into a [Deck]. Pass the target [format] so the commander fallback (first
     * card in the list when no `Commander` section is present) only kicks in for
     * commander-shaped formats. Pass null if the format is unknown — fallback is suppressed.
     */
    fun parse(text: String, format: DeckFormat? = null): Deck {
        var section = Section.DECK
        val cards = mutableListOf<String>()
        val commanders = mutableListOf<String>()
        var sawCommanderSection = false

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            // Allow a leading "//" comment or a "About" / "Name" header MTGA sometimes emits
            // — anything that doesn't look like a count + name line and isn't a section header
            // is silently ignored.
            when (line.lowercase()) {
                "deck", "main", "mainboard" -> { section = Section.DECK; continue }
                "commander", "commanders" -> { section = Section.COMMANDER; sawCommanderSection = true; continue }
                "sideboard" -> { section = Section.SIDEBOARD; continue }
                "companion" -> { section = Section.COMPANION; continue }
            }
            val m = LINE.matchEntire(line) ?: continue
            val count = m.groupValues[1].toInt()
            val name = stripSetSuffix(m.groupValues[2])
            when (section) {
                Section.DECK -> repeat(count) { cards += name }
                // One commander, or two for a partner pair (CR 702.124) — whether the two may
                // actually be paired is the deck validator's call, not the parser's.
                Section.COMMANDER -> if (name !in commanders) commanders += name
                Section.SIDEBOARD, Section.COMPANION -> Unit
            }
        }

        // First-card fallback for commander-shaped formats imported without a section header.
        // Only applied when the format definitively requires a commander; suppresses for unknown
        // format and for lists that already had an (empty) Commander header.
        if (commanders.isEmpty() && !sawCommanderSection &&
            format != null && format.isCommanderShape && cards.isNotEmpty()
        ) {
            commanders += cards.removeAt(0)
        }

        // No partner ability allows more than two commanders (CR 702.124g); a third listed one
        // goes back into the deck rather than vanishing, so the validator still sees all 100.
        cards += commanders.drop(2)
        return Deck(cards = cards, commander = commanders.getOrNull(0), secondCommander = commanders.getOrNull(1))
    }

    /**
     * Render [deck] as MTGA-format text. Always emits a `Deck` section; emits a `Commander`
     * section — one line per commander — only when [Deck.commanders] is non-empty.
     */
    fun serialize(deck: Deck): String = buildString {
        appendLine("Deck")
        deck.cards.groupingBy { it }.eachCount().forEach { (name, n) ->
            appendLine("$n $name")
        }
        if (deck.commanders.isNotEmpty()) {
            appendLine()
            appendLine("Commander")
            deck.commanders.forEach { appendLine("1 $it") }
        }
    }

    private fun stripSetSuffix(rawName: String): String =
        SET_SUFFIX.replace(rawName, "").trim()

    private enum class Section { DECK, COMMANDER, SIDEBOARD, COMPANION }
}
