package com.wingedsheep.mtg.sets.definitions.dominaria

import com.wingedsheep.mtg.sets.definitions.dominaria.cards.*

/**
 * Dominaria Set (2018)
 *
 * Dominaria was a standalone expansion set that returned to Magic's home plane,
 * featuring historic and legendary themes.
 *
 * Set Code: DOM
 * Release Date: April 27, 2018
 * Card Count: 280
 */
object DominariaSet {

    const val SET_CODE = "DOM"
    const val SET_NAME = "Dominaria"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        CastDown,
        Opt,
        RunAmok,
    )
}
