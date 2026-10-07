package com.wingedsheep.mtg.sets.definitions.ddn

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Duel Decks: Speed vs. Cunning (2014-09-05).
 * Scaffolded for Mardu Heart-Piercer's earliest printing and existing-card reprints.
 * A fixed-deck product, with the remaining cards still unauthored.
 */
object DuelDecksSpeedVsCunningSet : MtgSet {

    override val code = "DDN"
    override val displayName = "Duel Decks: Speed vs. Cunning"
    override val releaseDate = "2014-09-05"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ddn.cards"
}
