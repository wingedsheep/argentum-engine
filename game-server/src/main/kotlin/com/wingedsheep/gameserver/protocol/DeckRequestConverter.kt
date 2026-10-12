package com.wingedsheep.gameserver.protocol

import com.wingedsheep.sdk.model.CardEntry
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.PrintingRef

/**
 * Builds an SDK [Deck] from the wire format. Two paths:
 *
 * 1. Rich entries (`cardEntries: List<DeckEntryDTO>`). When non-null/empty this is
 *    authoritative; the legacy [deckList] map is ignored. Per-card [PrintingRef]s pin
 *    specific printings; the engine resolves them at game-init.
 *
 * 2. Legacy flat map (`deckList: Map<String, Int>`). Names + counts only; no per-card
 *    printing pinning. Each `(name, count)` pair fans out to `count` library entries.
 *
 * For commander-shape formats, [commander] names the commander card and
 * [commanderPrinting] optionally pins its printing; a partner pair (CR 702.124) adds
 * `secondCommander` / `secondCommanderPrinting`. The commanders do not appear in the
 * library entries — engine `GameInitializer` puts them in the command zone separately.
 *
 * A [companion] (CR 702.139) names a sideboard card the deck reveals before the game. It is a card
 * outside the game, so it rides on the sideboard: when the submitted sideboard doesn't list it
 * (a Commander deck has no other sideboard, CR 903.5e) one copy is added.
 */
object DeckRequestConverter {

    fun toDeck(
        deckList: Map<String, Int>,
        cardEntries: List<DeckEntryDTO>? = null,
        commander: String? = null,
        commanderPrinting: PrintingRef? = null,
        sideboard: Map<String, Int> = emptyMap(),
        secondCommander: String? = null,
        secondCommanderPrinting: PrintingRef? = null,
        companion: String? = null,
    ): Deck {
        // Sideboard cards ("outside the game", CR 100.4) are name-only here — wish fetches don't
        // depend on printing, and the engine seeds the SIDEBOARD zone from these names.
        val companionName = companion?.takeIf { it.isNotBlank() }
        val withCompanion = if (companionName == null || (sideboard[companionName] ?: 0) > 0) sideboard
        else sideboard + (companionName to 1)
        val sideboardEntries = withCompanion.flatMap { (name, count) -> List(count) { CardEntry(name) } }
        val rich = cardEntries.takeUnless { it.isNullOrEmpty() }
        val second = secondCommander.takeIf { commander != null }
        return if (rich != null) {
            Deck.fromEntries(
                entries = rich.map { CardEntry(it.name, it.printing) },
                commander = commander,
                commanderPrinting = commanderPrinting,
                sideboard = sideboardEntries,
                secondCommander = second,
                secondCommanderPrinting = secondCommanderPrinting.takeIf { second != null },
                companion = companionName,
            )
        } else {
            val cards = deckList.flatMap { (name, count) -> List(count) { name } }
            Deck(
                cards = cards,
                commander = commander,
                commanderPrinting = commanderPrinting,
                sideboard = sideboardEntries,
                secondCommander = second,
                secondCommanderPrinting = secondCommanderPrinting.takeIf { second != null },
                companion = companionName,
            )
        }
    }

    /**
     * A commander-shaped submission in the wire convention — [deckList] and [cardEntries] count
     * each commander once, the way the deck picker emits the merged deck — turned into a [Deck]
     * whose library excludes the commanders (the engine's and the validator's convention). One
     * copy of each commander is taken out of both the flat list and the rich entries; for the rich
     * entries, the copy pinned to the commander's own printing goes first.
     */
    fun toCommanderDeck(
        deckList: Map<String, Int>,
        cardEntries: List<DeckEntryDTO>?,
        commander: String,
        commanderPrinting: PrintingRef? = null,
        secondCommander: String? = null,
        secondCommanderPrinting: PrintingRef? = null,
        sideboard: Map<String, Int> = emptyMap(),
        companion: String? = null,
    ): Deck {
        val commanders = listOfNotNull(commander, secondCommander)
        val printings = mapOf(commander to commanderPrinting, secondCommander to secondCommanderPrinting)
        var entries = cardEntries.orEmpty()
        for (name in commanders) {
            val exact = entries.indexOfFirst { it.name == name && it.printing == printings[name] }
            val index = if (exact >= 0) exact else entries.indexOfFirst { it.name == name }
            if (index >= 0) entries = entries.filterIndexed { i, _ -> i != index }
        }
        return toDeck(
            deckList = stripCommanders(deckList, commanders),
            cardEntries = entries,
            commander = commander,
            commanderPrinting = commanderPrinting,
            sideboard = sideboard,
            secondCommander = secondCommander,
            secondCommanderPrinting = secondCommanderPrinting,
            companion = companion,
        )
    }

    /**
     * Subtract one copy of each of [commanders] from [deckList]. The wire format and lobby state
     * keep the commanders counted in the deck list; the engine's `Deck.cards` excludes them.
     * Mirrors the web client's `stripCommanderFromCards`. Idempotent for a commander that isn't
     * present.
     */
    fun stripCommanders(deckList: Map<String, Int>, commanders: List<String>): Map<String, Int> {
        if (commanders.none { it in deckList }) return deckList
        val next = deckList.toMutableMap()
        for (commander in commanders) {
            val current = next[commander] ?: continue
            if (current <= 1) next.remove(commander) else next[commander] = current - 1
        }
        return next
    }
}
