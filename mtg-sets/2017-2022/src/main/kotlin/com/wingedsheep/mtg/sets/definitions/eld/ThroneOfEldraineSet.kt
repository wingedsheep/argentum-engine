package com.wingedsheep.mtg.sets.definitions.eld

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Throne of Eldraine (2019)
 *
 * Set Code: ELD
 * Release Date: October 4, 2019
 *
 * Complete: every card in the set is implemented. The draft booster is the era default
 * (10 commons, 3 uncommons, 1 rare or mythic; basics supplied at deck building).
 */
object ThroneOfEldraineSet : MtgSet {

    override val code = "ELD"
    override val displayName = "Throne of Eldraine"
    override val releaseDate = "2019-10-04"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.eld.cards"
}
