package com.wingedsheep.mtg.sets.definitions.nph

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * New Phyrexia (2011)
 *
 * Set Code: NPH
 * Release Date: May 13, 2011
 *
 * Scaffolded to hold the canonical [CardDefinition] for Torpor Orb (its earliest real printing).
 * Cards and reprint rows auto-register via the classpath scan over [CARDS_PACKAGE].
 */
object NewPhyrexiaSet : MtgSet {

    override val code = "NPH"
    override val displayName = "New Phyrexia"
    override val releaseDate = "2011-05-13"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.nph.cards"
}
