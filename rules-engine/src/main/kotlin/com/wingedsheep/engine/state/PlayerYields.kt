package com.wingedsheep.engine.state

import com.wingedsheep.sdk.scripting.AbilityIdentity
import kotlinx.serialization.Serializable

/**
 * A single player's remembered "yield" preferences, keyed by [AbilityIdentity] (so a preference set
 * once applies to every current and future copy/instance of that card ability — exactly like MTGO's
 * right-click yields). See `backlog/stack-collapse-and-batch-decisions.md` §C.
 *
 * Three independent dimensions:
 *  - [untilEndOfTurn] — auto-pass priority on this ability's stack objects for the rest of the turn
 *    (cleared at every cleanup step, CR 514).
 *  - [wholeGame] — auto-pass priority on this ability's stack objects for the rest of the game.
 *  - [autoAnswer] — auto-resolve the ability's optional ("you may") may-question without prompting;
 *    `true` = always yes, `false` = always no. Whole-game scoped, matching MTGO "Always yes/no".
 *
 * Yields only ever *auto-pass* or *auto-answer the controller's own may-question*. They never make a
 * targeting, ordering, or modal choice on the player's behalf (§C.6).
 */
@Serializable
data class PlayerYields(
    val untilEndOfTurn: Set<AbilityIdentity> = emptySet(),
    val wholeGame: Set<AbilityIdentity> = emptySet(),
    val autoAnswer: Map<AbilityIdentity, Boolean> = emptyMap(),
) {
    /** True when priority on [identity]'s stack objects should be auto-passed (either scope). */
    fun isYieldingTo(identity: AbilityIdentity): Boolean =
        identity in untilEndOfTurn || identity in wholeGame

    /** The remembered may-question answer for [identity], or null if none is set. */
    fun answerFor(identity: AbilityIdentity): Boolean? = autoAnswer[identity]

    /** Nothing remembered at all — used to drop empty per-player entries from the map. */
    val isEmpty: Boolean
        get() = untilEndOfTurn.isEmpty() && wholeGame.isEmpty() && autoAnswer.isEmpty()

    companion object {
        val EMPTY = PlayerYields()
    }
}

/**
 * The kind of yield a player is setting, mirroring MTGO's four right-click options. Carried on the
 * client→server message and applied by [GameState.withYield].
 */
@Serializable
enum class YieldKind {
    /** Auto-pass priority on the ability's stack objects until end of turn. */
    YIELD_UNTIL_END_OF_TURN,

    /** Auto-pass priority on the ability's stack objects for the rest of the game. */
    YIELD_WHOLE_GAME,

    /** Always answer "yes" to the ability's optional may-question (whole game). */
    ALWAYS_ANSWER_YES,

    /** Always answer "no" to the ability's optional may-question (whole game). */
    ALWAYS_ANSWER_NO,
}
