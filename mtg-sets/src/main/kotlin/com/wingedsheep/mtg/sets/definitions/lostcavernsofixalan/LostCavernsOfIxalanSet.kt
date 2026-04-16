package com.wingedsheep.mtg.sets.definitions.lostcavernsofixalan

import com.wingedsheep.mtg.sets.definitions.lostcavernsofixalan.cards.*

/**
 * The Lost Caverns of Ixalan Set (2023)
 *
 * Set Code: LCI
 * Release Date: November 17, 2023
 */
object LostCavernsOfIxalanSet {

    const val SET_CODE = "LCI"
    const val SET_NAME = "The Lost Caverns of Ixalan"

    val basicLands = emptyList<com.wingedsheep.sdk.model.CardDefinition>()

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        MalcolmAlluringScoundrel,
    )
}
