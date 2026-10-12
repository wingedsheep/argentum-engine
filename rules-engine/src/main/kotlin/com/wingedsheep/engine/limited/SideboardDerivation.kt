package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.model.CardDefinition

/**
 * Derives a Limited player's sideboard from their card pool (CR 100.4b):
 *
 * > "In limited play involving individual players, all cards in a player's card pool not included
 * > in their deck are in that player's sideboard."
 *
 * So there is no separate sideboard editor in Sealed/Draft — every opened or drafted card the
 * player didn't run is automatically their sideboard, and wish effects (Burning Wish, …) draw on
 * exactly that. The complement is computed server-side at deck-submission time (the only point the
 * full pool is in scope), counted by card name.
 *
 * Basic lands are excluded: they're an unlimited resource in Limited deckbuilding, not finite pool
 * cards, so "leftover" basics aren't meaningful sideboard material.
 */
object SideboardDerivation {

    private val BASIC_LAND_NAMES = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    /**
     * @param pool the player's full sealed/drafted card pool (one entry per physical card)
     * @param maindeck the submitted deck as card-name → count (includes basics they chose to run)
     * @return sideboard as card-name → count: `poolCount(name) − deckCount(name)`, clamped at 0,
     *         excluding basic lands. Empty when the deck uses the whole non-basic pool.
     */
    fun fromPool(pool: List<CardDefinition>, maindeck: Map<String, Int>): Map<String, Int> {
        val poolCounts = pool
            .map { it.name }
            .filterNot { it in BASIC_LAND_NAMES }
            .groupingBy { it }
            .eachCount()

        return poolCounts
            .mapValues { (name, available) -> available - (maindeck[name] ?: 0) }
            .filterValues { it > 0 }
    }
}
