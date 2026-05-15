package com.wingedsheep.mtg.sets.definitions.khm

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Kaldheim Set (2021)
 *
 * Kaldheim is a Norse mythology-inspired plane themed around its ten
 * realms, with sagas, modal double-faced cards, and the Boast mechanic.
 *
 * Set Code: KHM
 * Release Date: February 5, 2021
 * Card Count: 285
 */
object KaldheimSet : MtgSet {

    override val code = "KHM"
    override val displayName = "Kaldheim"
    override val block = "Kaldheim"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.khm.cards"
}
