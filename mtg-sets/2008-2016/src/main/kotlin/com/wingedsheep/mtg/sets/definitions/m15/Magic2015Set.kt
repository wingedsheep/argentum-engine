package com.wingedsheep.mtg.sets.definitions.m15

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic 2015 (2014)
 *
 * Every card in the set is implemented; reprints are `Printing` rows pointing at their
 * canonical earliest printing.
 *
 * Set Code: M15
 * Release Date: 2014-07-18
 */
object Magic2015Set : MtgSet {

    override val code = "M15"
    override val displayName = "Magic 2015"
    override val releaseDate = "2014-07-18"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (rares print twice on the sheet,
     * mythics once: 53 rares and 15 mythics make the upgrade 15 in 121), plus a basic land from a
     * basic-only land slot, supplied at deck building. The foil that replaced a common in 9 packs
     * in 40 is cosmetic and not modelled. The 15 Sample Deck cards (#270-284) never opened in
     * packs, so their `Printing` rows are `inBooster = false`. Figures from mtg.wtf/pack/m15
     * (taw/magic-search-engine) and Scryfall's `is:booster`.
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m15.cards"
}
