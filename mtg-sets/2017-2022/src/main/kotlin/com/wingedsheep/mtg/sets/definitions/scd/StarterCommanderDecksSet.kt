package com.wingedsheep.mtg.sets.definitions.scd

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Starter Commander Decks
 *
 * Scaffolded to host the earliest real printing of cards later reprinted in other sets (e.g.
 * Sphinx of Enlightenment, reprinted in Bloomburrow Commander — its Game Night 2019 printing is a
 * box set, which doesn't count as a real printing). Intentionally incomplete relative to the
 * official product — only cards with a canonical [CardDefinition] or a [Printing] row here are
 * present.
 *
 * Set Code: SCD
 * Release Date: 2022-12-02
 */
object StarterCommanderDecksSet : MtgSet {

    override val code = "SCD"
    override val displayName = "Starter Commander Decks"
    override val releaseDate = "2022-12-02"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.scd.cards"
}
