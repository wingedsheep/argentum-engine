package com.wingedsheep.mtg.sets.definitions.znc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Zendikar Rising Commander
 *
 * Set Code: ZNC
 */
object ZendikarRisingCommanderSet : MtgSet {

    override val code = "ZNC"
    override val displayName = "Zendikar Rising Commander"
    override val releaseDate = "2020-09-25"
    override val incomplete = true
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.znc.cards"
}
