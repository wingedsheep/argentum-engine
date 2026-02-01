package com.wingedsheep.mtg.sets.definitions.onslaught

import com.wingedsheep.mtg.sets.definitions.onslaught.cards.*

/**
 * Onslaught Set (2002)
 *
 * Onslaught was the first set in the Onslaught block, featuring tribal themes
 * and introducing mechanics like Morph and Cycling.
 *
 * Set Code: ONS
 * Release Date: October 7, 2002
 * Card Count: 350
 */
object OnslaughtSet {

    const val SET_CODE = "ONS"
    const val SET_NAME = "Onslaught"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // White creatures
        GlorySeeker,

        // Black creatures and spells
        FesteringGoblin,
        Smother,

        // Red creatures and spells
        GoblinSledder,
        Shock,

        // Green creatures
        BarkhideMauler,
        ElvishVanguard,
        ElvishWarrior,
        Wellwisher,
        WirewoodElf,
        WirewoodSavage,
    )
}
