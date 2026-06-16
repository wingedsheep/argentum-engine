package com.wingedsheep.engine.mechanics.sba

/**
 * Ordering constants for state-based action checks.
 * Lower values run first. Gaps of 100 allow easy insertion of new checks.
 */
object SbaOrder {
    const val PLAYER_LIFE_LOSS = 100        // 704.5a
    const val COMMANDER_DAMAGE_LOSS = 150   // 704.5c (Commander format)
    const val POISON_LOSS = 200             // 704.5b
    const val DURATION_EXPIRY = 240         // 611.2b ("for as long as" durations end one-way)
    const val ATTACHED_COPY_EXPIRY = 245    // 611.2b ("becomes a copy for as long as attached")
    const val CONTROL_CHANGED_COMBAT = 250  // 506.4 (controller change removes from combat)
    const val ZERO_TOUGHNESS = 300          // 704.5f
    const val LETHAL_DAMAGE = 400           // 704.5g/h
    const val PLANESWALKER_LOYALTY = 500    // 704.5i
    const val LEGEND_RULE = 600             // 704.5j
    const val COUNTER_ANNIHILATION = 700    // 704.5q
    const val UNATTACHED_AURAS = 800        // 704.5m/n/p
    const val SAGA_SACRIFICE = 900          // 714.4
    const val COMMANDER_ZONE_CHOICE = 950   // 903.9a (Commander format)
    const val TOKENS_IN_WRONG_ZONES = 1000  // 704.5s
    const val PHANTOM_CARD_COPIES = 1050    // 707.10a
    const val TEAM_LOSS_PROPAGATION = 8500  // 810.8a (Two-Headed Giant: a team loses together)
    const val LEAVE_GAME = 9000             // 800.4a–c (multiplayer: process a player who has left)
    const val GAME_END = 9999
}
