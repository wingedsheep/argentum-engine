package com.wingedsheep.mtg.sets.definitions.p02

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Portal Second Age (1998)
 *
 * The second Portal introductory set. Scaffolded here to hold the canonical
 * [CardDefinition]s of cards whose earliest real-expansion printing is P02 (e.g.
 * Angel of Mercy, Ravenous Rats), with later sets contributing reprint [Printing] rows.
 *
 * Set Code: P02
 * Release Date: June 24, 1998
 */
object PortalSecondAgeSet : MtgSet {

    override val code = "P02"
    override val displayName = "Portal Second Age"
    override val releaseDate = "1998-06-24"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.p02.cards"
}
