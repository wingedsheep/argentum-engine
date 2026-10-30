package com.wingedsheep.mtg.sets.definitions.who

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Doctor Who (2023)
 *
 * Universes Beyond Commander set, not a draftable expansion. Scaffolded to hold the canonical
 * [CardDefinition] for cards that first appeared here (e.g. Barbara Wright, added in a later PR).
 *
 * Set Code: WHO
 * Release Date: October 13, 2023
 */
object DoctorWhoSet : MtgSet {

    override val code = "WHO"
    override val displayName = "Doctor Who"
    override val releaseDate = "2023-10-13"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.who.cards"
}
