package com.wingedsheep.mtg.sets.definitions.ddj

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Duel Decks: Izzet vs. Golgari (2012)
 *
 * Scaffolded so that Goblin Electromancer — first printed here, a year before its Dragon's Maze
 * reprint — can hold its canonical [CardDefinition] in its earliest real printing. The cards this
 * product reprints that are already implemented carry [Printing] rows here; the rest is still
 * unauthored.
 *
 * [sealedSupported] stays false: a duel deck is two fixed 60-card decks, not a draftable product.
 *
 * Set Code: DDJ
 * Release Date: 2012-09-07
 */
object DuelDecksIzzetVsGolgariSet : MtgSet {

    override val code = "DDJ"
    override val displayName = "Duel Decks: Izzet vs. Golgari"
    override val releaseDate = "2012-09-07"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ddj.cards"
}
