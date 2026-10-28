package com.wingedsheep.mtg.sets.definitions.spe

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Marvel's Spider-Man Eternal (2025)
 *
 * The Eternal companion product to Marvel's Spider-Man.
 * Scaffolded here as the canonical home for cards whose earliest real printing is
 * SPE. Intentionally incomplete relative to the official set — only cards
 * relocated here as their canonical earliest printing live in this package.
 *
 * Set Code: SPE
 * Release Date: 2025-09-26
 */
object MarvelsSpiderManEternalSet : MtgSet {

    override val code = "SPE"
    override val displayName = "Marvel's Spider-Man Eternal"
    override val releaseDate = "2025-09-26"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.spe.cards"
}
