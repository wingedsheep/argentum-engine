package com.wingedsheep.mtg.sets.definitions.tmc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Teenage Mutant Ninja Turtles Eternal (2026)
 *
 * The eternal-legal supplemental cards released alongside Teenage Mutant Ninja Turtles.
 * Scaffolded here as the canonical home for cards whose earliest real printing is
 * TMC. Intentionally incomplete relative to the official set — only cards
 * relocated here as their canonical earliest printing live in this package.
 *
 * Set Code: TMC
 * Release Date: 2026-03-06
 */
object TeenageMutantNinjaTurtlesEternalSet : MtgSet {

    override val code = "TMC"
    override val displayName = "Teenage Mutant Ninja Turtles Eternal"
    override val releaseDate = "2026-03-06"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.tmc.cards"
}
