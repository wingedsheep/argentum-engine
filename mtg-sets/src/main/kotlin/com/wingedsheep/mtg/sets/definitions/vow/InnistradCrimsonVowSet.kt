package com.wingedsheep.mtg.sets.definitions.vow

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Innistrad: Crimson Vow (2021)
 *
 * Set Code: VOW
 * Release Date: November 19, 2021
 */
object InnistradCrimsonVowSet : MtgSet {

    override val code = "VOW"
    override val displayName = "Innistrad: Crimson Vow"
    override val releaseDate = "2021-11-19"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.vow.cards"
}
