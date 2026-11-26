package com.wingedsheep.mtg.sets.definitions.m13

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic 2013 (2012)
 *
 * Set Code: M13
 * Release Date: 2012-07-13
 */
object Magic2013Set : MtgSet {

    override val code = "M13"
    override val displayName = "Magic 2013"
    override val releaseDate = "2012-07-13"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (rares print twice on the sheet,
     * mythics once: 53 rares and 15 mythics make the upgrade 15 in 121), plus a basic land from a
     * basic-only land slot, supplied at deck building. The foil that replaced a common is cosmetic
     * and not modelled. Every non-basic card opens (Scryfall `set:m13 is:booster -t:basic` is
     * 101 / 60 / 53 / 15). Figures from mtg.wtf/pack/m13 (taw/magic-search-engine) and Scryfall.
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 15.0 / 121)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m13.cards"
}
