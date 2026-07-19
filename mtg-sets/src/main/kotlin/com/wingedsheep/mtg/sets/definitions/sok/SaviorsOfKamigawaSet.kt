package com.wingedsheep.mtg.sets.definitions.sok

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Saviors of Kamigawa (2005)
 *
 * The final set of the Kamigawa block. Scaffolded here as the canonical home for cards whose
 * earliest real printing is SOK (e.g. Hidetsugu's Second Rite, reprinted in Foundations).
 * Intentionally incomplete relative to the official set — only cards relocated here as their
 * canonical earliest printing live in this package.
 *
 * Set Code: SOK
 * Release Date: 2005-06-03
 */
object SaviorsOfKamigawaSet : MtgSet {

    override val code = "SOK"
    override val displayName = "Saviors of Kamigawa"
    override val releaseDate = "2005-06-03"
    override val block = "Kamigawa"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.sok.cards"
}
