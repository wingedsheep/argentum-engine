package com.wingedsheep.mtg.sets.definitions.tdm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

/**
 * Tarkir: Dragonstorm (2025)
 *
 * Set Code: TDM
 * Release Date: April 11, 2025
 */
object TarkirDragonstormSet : MtgSet {

    override val code = "TDM"
    override val displayName = "Tarkir: Dragonstorm"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.tdm.cards"
}
