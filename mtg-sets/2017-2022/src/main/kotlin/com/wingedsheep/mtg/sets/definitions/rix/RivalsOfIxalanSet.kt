package com.wingedsheep.mtg.sets.definitions.rix

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Rivals of Ixalan (2018)
 *
 * Set Code: RIX
 * Release Date: January 19, 2018
 */
object RivalsOfIxalanSet : MtgSet {

    override val code = "RIX"
    override val displayName = "Rivals of Ixalan"
    override val releaseDate = "2018-01-19"

    /**
     * Draft booster: ten commons, three uncommons, one rare or mythic and a basic land (the land is
     * supplied at deck building; the foil that replaces a common is cosmetic and not modelled).
     * The rare sheet prints each of the 48 rares twice and each of the 13 mythics once, so a pack's
     * rare slot is mythic 13 times in 109. The planeswalker-deck cards (#197–205) are `inBooster = false`.
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 13.0 / 109)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Scryfall's `trix` sync brings back only Rivals of Ixalan's Elemental, Saproling and Golem,
     * so its Treasure is self-hosted under `web-client/public/images/tokens/`.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(name = "Treasure", imageUri = "/images/tokens/rix-treasure.jpeg"),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.rix.cards"
}
