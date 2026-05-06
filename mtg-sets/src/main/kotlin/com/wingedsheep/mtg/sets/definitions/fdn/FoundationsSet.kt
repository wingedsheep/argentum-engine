package com.wingedsheep.mtg.sets.definitions.fdn

import com.wingedsheep.mtg.sets.definitions.fdn.cards.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

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
object FoundationsSet : MtgSet {

    override val code = "FDN"
    override val displayName = "Foundations"



    /**
     * All cards implemented from this set.
     */
    override val cards: List<CardDefinition> = listOf(
        Bushwhack,
        MossbornHydra,
        Negate,
        OrdealOfNylea,
        SnakeskinVeil,
        SpringbloomDruid,
    )
}
