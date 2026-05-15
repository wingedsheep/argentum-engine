package com.wingedsheep.mtg.sets.definitions.lea

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

/**
 * Limited Edition Alpha (1993)
 *
 * The first Magic: The Gathering set ever printed. Most cards in modern sets
 * that originally appeared here (Sol Ring, Counterspell, dual lands, etc.) have
 * their canonical [CardDefinition] in this package; later printings contribute
 * only `Printing` rows.
 *
 * Set Code: LEA
 * Release Date: August 5, 1993
 * Card Count: 295
 */
object AlphaSet : MtgSet {

    override val code = "LEA"
    override val displayName = "Limited Edition Alpha"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.lea.cards"
}
