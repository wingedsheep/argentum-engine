package com.wingedsheep.mtg.sets.definitions.bfz

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Battle for Zendikar (2015)
 *
 * Set Code: BFZ
 * Release Date: October 2, 2015
 */
object BattleForZendikarSet : MtgSet {

    override val code = "BFZ"
    override val displayName = "Battle for Zendikar"
    override val releaseDate = "2015-10-02"
    override val block = "Battle for Zendikar"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    /** Battle for Zendikar's own basics: one representative full-art printing per type. */
    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.bfz.cards"
}
