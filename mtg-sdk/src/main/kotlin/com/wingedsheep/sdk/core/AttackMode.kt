package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * Which opponents a creature may attack in a multiplayer game — one of the mutually exclusive
 * multiplayer attack options (CR 802 / 803). A Free-for-All game must use exactly one of these
 * (CR 806.2b). Orthogonal to [Format]: it's a *rules option* for a single game, chosen in the
 * lobby, not a deck-construction or win-condition concept.
 *
 * In a two-player game all three behave identically (the sole opponent is simultaneously "to the
 * left", "to the right", and the only choice), so this only changes behaviour with 3+ seats.
 * "Left"/"right" are measured in seating (= turn) order: turn order proceeds to each player's left
 * (CR 103.7b), so the player to your left is the next seat in turn order and the player to your
 * right is the previous seat.
 */
@Serializable
enum class AttackMode {
    /** CR 802 — every opponent is a defending player; each attacker is declared against a
     *  specific opponent (or a planeswalker/battle they control/protect). The default. */
    MULTIPLE,

    /** CR 803.1a — a creature may attack only the opponent seated immediately to its
     *  controller's left (or a planeswalker/battle that opponent controls/protects). */
    LEFT,

    /** CR 803.1b — a creature may attack only the opponent seated immediately to its
     *  controller's right (or a planeswalker/battle that opponent controls/protects). */
    RIGHT,
}
