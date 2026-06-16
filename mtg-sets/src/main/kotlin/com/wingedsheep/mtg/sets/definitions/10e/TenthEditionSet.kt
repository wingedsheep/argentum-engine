package com.wingedsheep.mtg.sets.definitions.`10e`

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Tenth Edition (2007)
 *
 * Set Code: 10E
 * Release Date: July 13, 2007
 *
 * The tenth core set — an all-reprint set, so every card lives canonically in an earlier set and
 * contributes a [Printing] row here. No card debuts in 10E, so this package holds only reprint
 * rows (auto-generated from the mtgish IR via `:mtgish-tooling`) plus the curated basic lands.
 */
object TenthEditionSet : MtgSet {

    override val code = "10E"
    override val displayName = "Tenth Edition"
    override val releaseDate = "2007-07-13"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.10e.cards"
}
