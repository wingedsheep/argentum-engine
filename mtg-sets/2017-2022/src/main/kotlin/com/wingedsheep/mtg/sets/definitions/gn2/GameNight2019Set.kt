package com.wingedsheep.mtg.sets.definitions.gn2

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Game Night 2019
 *
 * Scaffolded to host the earliest real printing of its new cards that were later reprinted in
 * other sets (e.g. Sphinx of Enlightenment, reprinted in Bloomburrow Commander). Intentionally
 * incomplete relative to the official product — only cards with a canonical [CardDefinition] or a
 * [Printing] row here are present.
 *
 * Set Code: GN2
 * Release Date: 2019-11-15
 */
object GameNight2019Set : MtgSet {

    override val code = "GN2"
    override val displayName = "Game Night 2019"
    override val releaseDate = "2019-11-15"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.gn2.cards"
}
