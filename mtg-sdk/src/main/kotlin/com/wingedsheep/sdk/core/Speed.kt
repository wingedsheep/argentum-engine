package com.wingedsheep.sdk.core

/**
 * The **speed** a player can have (Aetherdrift, CR 702.179).
 *
 * Speed is a per-player integer designation, not a counter and not a resource that can be spent:
 *
 * - Players start with *no* speed (CR 702.179b). "No speed" and "speed 0" are the same value here —
 *   CR 702.179f says an effect that reads a player's speed sees 0 when they have none, and
 *   CR 702.179c says increasing the speed of a player who has none simply sets it to that amount.
 *   Both collapse onto plain integer arithmetic from [NONE], so the engine models speed as a single
 *   `Int` rather than a nullable one.
 * - Speed never exceeds [MAX]: the inherent speed trigger (CR 702.179d) carries an "if your speed is
 *   less than 4" intervening-if, and the engine clamps besides — which is what lets "max speed"
 *   (CR 702.179e) be a plain equality test rather than a `>=`.
 * - Speed *usually* only rises, but it is not monotonic. Spikeshell Harrier reduces an opponent's
 *   speed by 1, so the vocabulary is a signed change
 *   ([com.wingedsheep.sdk.scripting.effects.ChangeSpeedEffect]) with a per-effect floor, not an
 *   increase-only primitive.
 * - *Having* speed, though, is permanent: once a player has any speed they keep the designation for
 *   the rest of the game, the way the city's blessing is never lost. Nothing drops a player back to
 *   [NONE] — the one reducing effect carries its own "can't reduce their speed below 1" rider — so
 *   there is no cleanup step for speed.
 */
object Speed {

    /** A player who has never had their speed set (CR 702.179b) — reads as 0 (CR 702.179f). */
    const val NONE: Int = 0

    /** The speed a permanent with "Start your engines!" confers (CR 702.179a / 704.5z). */
    const val STARTING: Int = 1

    /** "Max speed": a player has it if and only if their speed is exactly this (CR 702.179e). */
    const val MAX: Int = 4
}
