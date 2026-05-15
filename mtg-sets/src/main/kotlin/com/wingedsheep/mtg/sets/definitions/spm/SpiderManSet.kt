package com.wingedsheep.mtg.sets.definitions.spm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Marvel's Spider-Man (2025)
 *
 * Set Code: SPM
 * Release Date: September 26, 2025
 */
object SpiderManSet : MtgSet {

    override val code = "SPM"
    override val displayName = "Marvel's Spider-Man"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.spm.cards"
}
