package com.wingedsheep.engine.gym.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How to obtain a deck for a gym environment.
 *
 * Two shapes are supported:
 * - [Explicit]: constructed lists provided by the caller (e.g., a MageZero
 *   test run submitting its own decks).
 * - [RandomSealed]: generate a fresh sealed deck from a registered set using
 *   the heuristic deck builder. Requires a [com.wingedsheep.engine.ai.BoosterGenerator]
 *   to be wired into [DeckResolver].
 */
@Serializable
sealed interface DeckSpec {

    /**
     * Deck given as a `cardName → count` map. Card names may include art
     * variants in the `"Name#COLLECTOR"` form used elsewhere in the engine.
     */
    @Serializable
    @SerialName("Explicit")
    data class Explicit(val cards: Map<String, Int>) : DeckSpec

    /**
     * Generate a sealed deck on demand.
     *
     * @property setCode Which set to open boosters from. When null, a random
     *                   set is chosen from the booster generator's catalogue.
     * @property boosterCount Number of 15-card boosters to open (default 8,
     *                        matching tournament sealed).
     */
    @Serializable
    @SerialName("RandomSealed")
    data class RandomSealed(
        val setCode: String? = null,
        val boosterCount: Int = 8
    ) : DeckSpec
}
