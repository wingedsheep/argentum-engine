package com.wingedsheep.mtg.sets.definitions.msh

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Marvel Super Heroes (2026)
 *
 * Set Code: MSH
 * Release Date: June 26, 2026
 */
object MarvelSuperHeroesSet : MtgSet {

    override val code = "MSH"
    override val displayName = "Marvel Super Heroes"
    override val releaseDate = "2026-06-26"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.msh.cards"
}
