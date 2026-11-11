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
 * **Basic lands.** The Magic Tournament Rules' Limited deck-construction section lets a player add
 * any number of basic lands named Plains, Island, Swamp, Mountain or Forest to their deck *and
 * sideboard* — so a Living Wish, a Mastermind's Acquisition or The Holodeck can fetch one in
 * Limited. Every derived sideboard therefore carries [BASIC_LANDS_PER_TYPE] of each of those five,
 * the number the player registers on top of their pool (the rule fixes no count; the player picks
 * one). Pool copies of those five fold into the supply rather than adding to it: a basic opened in
 * a booster is the same free card the supply hands out. Snow-covered basics and Wastes are not
 * part of the supply (the rule excludes them), so a pool copy of one stays an ordinary pool card.
 */
object SideboardDerivation {

    /** The basic land names a Limited player may add freely (snow basics and Wastes excluded). */
    val BASIC_LAND_NAMES: Set<String> = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    /**
     * How many of each basic land every Limited sideboard holds. Each fetch takes one card, and the
     * repeatable one (The Holodeck) takes one a turn, so ten of each type — fifty basics — outlasts
     * any game while keeping the outside-the-game zone a few dozen entities, not hundreds.
     */
    const val BASIC_LANDS_PER_TYPE: Int = 10

    /** The basic land supply added to every Limited sideboard, card name → count. */
    val basicLandSupply: Map<String, Int> = BASIC_LAND_NAMES.associateWith { BASIC_LANDS_PER_TYPE }

    /** Whether [name] is one of the free basics the supply provides (not a pool card). */
    fun isSuppliedBasic(name: String): Boolean = name in BASIC_LAND_NAMES

    /**
     * @param pool the player's full sealed/drafted card pool (one entry per physical card)
     * @param maindeck the submitted deck as card-name → count (includes basics they chose to run)
     * @return sideboard as card-name → count: `poolCount(name) − deckCount(name)`, clamped at 0,
     *         for every pool card that isn't a supplied basic, followed by [basicLandSupply]. Holds
     *         only the basic supply when the deck uses the whole non-basic pool.
     */
    fun fromPool(pool: List<CardDefinition>, maindeck: Map<String, Int>): Map<String, Int> {
        val poolCounts = pool
            .map { it.name }
            .filterNot(::isSuppliedBasic)
            .groupingBy { it }
            .eachCount()

        return poolCounts
            .mapValues { (name, available) -> available - (maindeck[name] ?: 0) }
            .filterValues { it > 0 } + basicLandSupply
    }
}
