package com.wingedsheep.mtg.sets.definitions.hou

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Hour of Devastation (2017)
 *
 * mtgish-tooling auto-generated seed: only the cards relocated here as their canonical
 * earliest printing. Intentionally incomplete relative to the official set.
 *
 * Set Code: HOU
 * Release Date: 2017-07-14
 */
object HourOfDevastationSet : MtgSet {

    override val code = "HOU"
    override val displayName = "Hour of Devastation"
    override val releaseDate = "2017-07-14"
    override val block = "Amonkhet"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.hou.cards"
}
