package com.wingedsheep.mtg.sets.definitions.rtr

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Return to Ravnica (2012)
 *
 * Every card of the set is implemented; cards first printed elsewhere carry a `Printing` row.
 *
 * Set Code: RTR
 * Release Date: 2012-10-05
 */
object ReturnToRavnicaSet : MtgSet {

    override val code = "RTR"
    override val displayName = "Return to Ravnica"
    override val releaseDate = "2012-10-05"
    override val block = "Return to Ravnica"

    /**
     * Draft Booster: 10 commons (the five Guildgates among them), 3 uncommons and a rare or mythic
     * (53 rares print twice on the sheet, 15 mythics once: the upgrade is 15 in 121). The basic-land
     * slot is supplied at deck building, and the foil that replaced a common in 9 packs in 40 is
     * cosmetic and not modelled, nor is the common sheet's colour balancing. Figures from
     * mtg.wtf/pack/rtr (taw/magic-search-engine).
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.rtr.cards"
}
