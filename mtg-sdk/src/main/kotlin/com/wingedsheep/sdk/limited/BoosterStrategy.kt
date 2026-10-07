package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/**
 * Generates a single booster pack from a set's card pool.
 *
 * Sets pick a strategy via [com.wingedsheep.sdk.model.MtgSet.boosterStrategy].
 * Strategies are pure functions of (card pool, random) -> list of cards;
 * they do not know about the booster generator, set codes, or basic lands
 * (the generator filters basic lands out before calling).
 *
 * New strategies can be added without touching the engine by writing a new
 * class that implements this interface.
 */
fun interface BoosterStrategy {
    fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition>
}
