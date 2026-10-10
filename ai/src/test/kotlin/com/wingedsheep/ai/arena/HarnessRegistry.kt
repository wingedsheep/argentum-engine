package com.wingedsheep.ai.arena

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.model.MtgSet

/**
 * The card registry every simulated-game harness (arena, pod arena, game logs, benchmarks) plays
 * against: the given sets' cards and basic lands, plus [PredefinedTokens.allTokens].
 *
 * The tokens are not optional. `CreatePredefinedTokenExecutor` looks Food, Treasure, Clue, Map, the
 * empower Jace, … up by name and returns an effect *error* when the registry lacks them — which the
 * harnesses never surfaced, so a registry built from set cards alone silently played games in which
 * no predefined token was ever created. Registration order mirrors the game server's
 * `GameBeansConfig.cardRegistry()`: tokens first, then the sets.
 */
fun harnessRegistry(sets: Iterable<MtgSet>): CardRegistry = CardRegistry().apply {
    register(PredefinedTokens.allTokens)
    for (set in sets) {
        register(set.cards)
        register(set.basicLands)
    }
}

fun harnessRegistry(vararg sets: MtgSet): CardRegistry = harnessRegistry(sets.asIterable())
