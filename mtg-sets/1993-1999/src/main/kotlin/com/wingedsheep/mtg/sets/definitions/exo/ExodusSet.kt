package com.wingedsheep.mtg.sets.definitions.exo

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Exodus (1998)
 *
 * The third and final set in the Tempest block, set on the plane of Rath.
 * Exodus introduced the Buyback follow-up cards and the "tutor" enchantments,
 * and is home to staples like Survival of the Fittest and Recurring Nightmare.
 *
 * Set Code: EXO
 * Release Date: June 15, 1998
 * Card Count: 143
 */
object ExodusSet : MtgSet {

    override val code = "EXO"
    override val displayName = "Exodus"
    override val releaseDate = "1998-06-15"
    override val block = "Tempest"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.exo.cards"
}
