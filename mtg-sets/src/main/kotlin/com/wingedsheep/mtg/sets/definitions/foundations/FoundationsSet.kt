package com.wingedsheep.mtg.sets.definitions.foundations

import com.wingedsheep.mtg.sets.definitions.foundations.cards.*

/**
 * Foundations (2024)
 *
 * Set Code: FDN
 * Release Date: November 15, 2024
 *
 * Foundations is a reprint-focused core-style set, used here primarily as a home
 * for Modern-legal staples referenced by MageZero training decks (see
 * backlog/magezero-coverage.md).
 */
object FoundationsSet {

    const val SET_CODE = "FDN"
    const val SET_NAME = "Foundations"

    val basicLands = emptyList<com.wingedsheep.sdk.model.CardDefinition>()

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        Negate,
    )
}
