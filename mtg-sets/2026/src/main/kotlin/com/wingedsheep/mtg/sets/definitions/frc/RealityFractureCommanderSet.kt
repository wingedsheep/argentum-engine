package com.wingedsheep.mtg.sets.definitions.frc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Reality Fracture Commander (2026)
 *
 * Commander preconstructed decks released alongside Reality Fracture.
 *
 * Set Code: FRC
 * Release Date: October 2, 2026
 */
object RealityFractureCommanderSet : MtgSet {

    override val code = "FRC"
    override val displayName = "Reality Fracture Commander"
    override val releaseDate = "2026-10-02"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.frc.cards"
}
