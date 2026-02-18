package com.wingedsheep.mtg.sets.definitions.scourge

import com.wingedsheep.mtg.sets.definitions.scourge.cards.*

/**
 * Scourge Set (2003)
 *
 * Scourge was the third and final set in the Onslaught block, featuring
 * the Storm mechanic and heavy tribal themes including Dragons.
 *
 * Set Code: SCG
 * Release Date: May 26, 2003
 * Card Count: 143
 */
object ScourgeSet {

    const val SET_CODE = "SCG"
    const val SET_NAME = "Scourge"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // Artifacts
        ArkOfBlight,

        // Black/Red creatures
        BladewingTheRisen,

        // Black creatures
        CarrionFeeder,

        // Green creatures
        FierceEmpath,

        // Green sorceries
        BreakAsunder,

        // Red creatures
        GoblinWarchief,
        SiegeGangCommander,
    )
}
