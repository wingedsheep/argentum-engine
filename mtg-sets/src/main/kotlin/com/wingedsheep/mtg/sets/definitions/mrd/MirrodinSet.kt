package com.wingedsheep.mtg.sets.definitions.mrd

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Mirrodin Set (2003)
 *
 * First set in the Mirrodin block. Introduces the artifact-focused metal plane
 * of Mirrodin alongside mechanics like Affinity, Equipment, Imprint, and Entwine.
 *
 * Set Code: MRD
 * Release Date: October 2, 2003
 * Card Count: 306
 */
object MirrodinSet : MtgSet {

    override val code = "MRD"
    override val displayName = "Mirrodin"
    override val releaseDate = "2003-10-02"
    override val block = "Mirrodin"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mrd.cards"
}
