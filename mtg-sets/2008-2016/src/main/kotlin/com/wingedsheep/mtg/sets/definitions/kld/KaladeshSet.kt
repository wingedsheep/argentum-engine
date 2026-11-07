package com.wingedsheep.mtg.sets.definitions.kld

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Kaladesh (2016)
 *
 * Every card is implemented: canonicals for cards first printed here, `Printing` rows for
 * the reprints.
 *
 * Set Code: KLD
 * Release Date: 2016-09-30
 */
object KaladeshSet : MtgSet {

    override val code = "KLD"
    override val displayName = "Kaladesh"
    override val releaseDate = "2016-09-30"
    override val block = "Kaladesh"

    /**
     * Draft booster: ten commons, three uncommons, one rare or mythic and a basic land (the land is
     * supplied at deck building; the foil or Kaladesh Inventions masterpiece — about one pack in 144 —
     * that replaces a common is cosmetic and not modelled). The rare sheet prints each of the 53 rares
     * twice and each of the 15 mythics once, so a pack's rare slot is mythic 15 times in 121. The
     * Planeswalker Deck cards (#265–274) are `inBooster = false`, or reprint rows flagged the same.
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.kld.cards"
}
