package com.wingedsheep.mtg.sets.definitions.pip

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Fallout (2024)
 *
 * The Universes Beyond: Fallout Commander decks. Scaffolded to host the earliest real printing of
 * its new cards that were later reprinted in other sets (e.g.
 * Ferrous Lake and Viridescent Bog, reprinted in Bloomburrow Commander). Intentionally incomplete
 * relative to the official product — only cards with a canonical [CardDefinition] or a [Printing]
 * row here are present.
 *
 * Set Code: PIP
 * Release Date: 2024-03-08
 */
object FalloutCommanderSet : MtgSet {

    override val code = "PIP"
    override val displayName = "Fallout"
    override val releaseDate = "2024-03-08"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.pip.cards"
}
