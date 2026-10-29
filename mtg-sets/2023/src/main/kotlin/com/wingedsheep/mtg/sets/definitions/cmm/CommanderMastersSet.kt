package com.wingedsheep.mtg.sets.definitions.cmm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Commander Masters (2023)
 *
 * Commander-focused reprint set with a handful of new legends.
 * It is intentionally incomplete relative to the official set.
 *
 * Set Code: CMM
 * Release Date: August 4, 2023
 */
object CommanderMastersSet : MtgSet {

    override val code = "CMM"
    override val displayName = "Commander Masters"
    override val releaseDate = "2023-08-04"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.cmm.cards"
}
