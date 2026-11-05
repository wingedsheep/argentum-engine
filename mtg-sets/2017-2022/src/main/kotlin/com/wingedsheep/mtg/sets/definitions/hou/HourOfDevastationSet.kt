package com.wingedsheep.mtg.sets.definitions.hou

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Hour of Devastation (2017)
 *
 * Set Code: HOU
 * Release Date: 2017-07-14
 */
object HourOfDevastationSet : MtgSet {

    override val code = "HOU"
    override val displayName = "Hour of Devastation"
    override val releaseDate = "2017-07-14"
    override val block = "Amonkhet"

    /**
     * Draft booster: ten commons, three uncommons, one rare or mythic and a basic land (the land is
     * supplied at deck building; the foil or Amonkhet Invocations masterpiece that replaces a common
     * is cosmetic and not modelled). The rare sheet prints each of the 42 rares twice and each of the
     * 12 mythics once, so a pack's rare slot is mythic 12 times in 96. The Planeswalker Deck cards
     * (#200–209) are `inBooster = false`.
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 12.0 / 96)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.hou.cards"
}
