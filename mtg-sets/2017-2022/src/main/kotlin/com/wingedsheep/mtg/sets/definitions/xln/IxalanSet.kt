package com.wingedsheep.mtg.sets.definitions.xln

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Ixalan (2017)
 *
 * Set Code: XLN
 * Release Date: September 29, 2017
 */
object IxalanSet : MtgSet {

    override val code = "XLN"
    override val displayName = "Ixalan"
    override val releaseDate = "2017-09-29"

    /**
     * Draft booster: ten commons, three uncommons, one rare or mythic and a basic land (the land is
     * supplied at deck building; the foil that replaces a common is cosmetic and not modelled).
     * MTGJSON's XLN draft sheet prints each of the 63 rares twice and each of the 15 mythics once, so a
     * pack's rare slot is mythic 15 times in 141. The planeswalker-deck cards (#280–289) are
     * `inBooster = false`.
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 15.0 / 141)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.xln.cards"
}
