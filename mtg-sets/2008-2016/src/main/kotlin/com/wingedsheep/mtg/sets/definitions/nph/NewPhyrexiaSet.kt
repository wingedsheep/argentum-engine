package com.wingedsheep.mtg.sets.definitions.nph

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * New Phyrexia Set (2011)
 *
 * Third and final set in the Scars of Mirrodin block. Phyrexia's victory over Mirrodin;
 * introduces Phyrexian mana and the Living Weapon mechanic's continuation.
 *
 * Set Code: NPH
 * Release Date: May 13, 2011
 * Card Count: 175
 */
object NewPhyrexiaSet : MtgSet {

    override val code = "NPH"
    override val displayName = "New Phyrexia"
    override val releaseDate = "2011-05-13"
    override val block = "Scars of Mirrodin"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.nph.cards"
}
