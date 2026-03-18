package com.wingedsheep.engine.mechanics.sba

/**
 * Ordering constants for state-based action checks.
 * Lower values run first. Gaps of 100 allow easy insertion of new checks.
 */
object SbaOrder {
    const val PLAYER_LIFE_LOSS = 100        // 704.5a
    const val POISON_LOSS = 200             // 704.5b
    const val ZERO_TOUGHNESS = 300          // 704.5f
    const val LETHAL_DAMAGE = 400           // 704.5g/h
    const val PLANESWALKER_LOYALTY = 500    // 704.5i
    const val LEGEND_RULE = 600             // 704.5j
    const val COUNTER_ANNIHILATION = 700    // 704.5m
    const val UNATTACHED_AURAS = 800        // 704.5n/p
    const val SAGA_SACRIFICE = 900          // 714.4
    const val TOKENS_IN_WRONG_ZONES = 1000  // 704.5s
    const val GAME_END = 9999
}
