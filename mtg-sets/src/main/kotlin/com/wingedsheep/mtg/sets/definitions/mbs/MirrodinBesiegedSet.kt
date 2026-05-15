package com.wingedsheep.mtg.sets.definitions.mbs

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

/**
 * Mirrodin Besieged Set (2011)
 *
 * Second set in the Scars of Mirrodin block. Continues the Mirran vs. Phyrexian
 * war on the plane of Mirrodin and introduces Battle Cry and Living Weapon.
 *
 * Set Code: MBS
 * Release Date: February 4, 2011
 * Card Count: 155
 */
object MirrodinBesiegedSet : MtgSet {

    override val code = "MBS"
    override val displayName = "Mirrodin Besieged"
    override val block = "Scars of Mirrodin"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mbs.cards"
}
