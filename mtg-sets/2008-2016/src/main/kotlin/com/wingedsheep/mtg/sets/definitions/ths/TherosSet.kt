package com.wingedsheep.mtg.sets.definitions.ths

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Theros (2013)
 *
 * Set Code: THS
 * Release Date: 2013-09-27
 */
object TherosSet : MtgSet {

    override val code = "THS"
    override val displayName = "Theros"
    override val releaseDate = "2013-09-27"
    override val block = "Theros"

    /**
     * Draft Booster (mtg.wtf/pack/ths): 10 commons, 3 uncommons, a rare or mythic (53 rares print
     * twice on the sheet, 15 mythics once: the upgrade is 15 in 121) and a basic land, supplied at
     * deck building. The foil replacing a common in 9 packs of 40 is cosmetic and not modelled.
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ths.cards"
}
