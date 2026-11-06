package com.wingedsheep.mtg.sets.definitions.akh

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Amonkhet (2017)
 *
 * Set Code: AKH
 * Release Date: April 28, 2017
 */
object AmonkhetSet : MtgSet {

    override val code = "AKH"
    override val displayName = "Amonkhet"
    override val releaseDate = "2017-04-28"
    override val block = "Amonkhet"

    /**
     * Draft booster: ten commons, three uncommons, one rare or mythic and a basic land (the land is
     * supplied at deck building; the foil or Amonkhet Invocations masterpiece that replaces a common
     * is cosmetic and not modelled). The rare sheet prints each of the 53 rares twice and each of the
     * 15 mythics once, so a pack's rare slot is mythic 15 times in 121. The Planeswalker Deck cards
     * (#270–287) are `inBooster = false` or reprint rows.
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.akh.cards"
}
