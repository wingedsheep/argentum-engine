package com.wingedsheep.mtg.sets.definitions.`8ed`

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Eighth Edition (2003)
 *
 * Set Code: 8ED
 * Release Date: July 28, 2003
 *
 * The eighth core set — the first to use the modern card frame. An all-reprint
 * core set, so most cards live canonically in earlier sets and contribute a
 * [Printing] row here; only cards whose earliest printing is 8ED get a full
 * `card(...)` definition.
 */
object EighthEditionSet : MtgSet {

    override val code = "8ED"
    override val displayName = "Eighth Edition"
    override val releaseDate = "2003-07-28"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE).map { it.copy(setCode = code) }
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.8ed.cards"
}
