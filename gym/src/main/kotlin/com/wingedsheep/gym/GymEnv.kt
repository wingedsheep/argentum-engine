package com.wingedsheep.gym

import com.wingedsheep.gym.contract.ObservationResult

/**
 * A self-contained gym environment the service layer can drive uniformly.
 *
 * Two implementations exist:
 * - [GameGymEnv] — a game of Magic (wraps [GameEnvironment]).
 * - [com.wingedsheep.gym.deckbuild.DeckbuildEnvironment] — turning a sealed pool into a deck.
 *
 * Each env owns its own action bookkeeping: `observe` produces the observation an agent
 * acts on, and `step` resolves an action ID *from the most recent observation* and advances.
 * Game-specific operations (decision submission, snapshot/restore, reset) live on
 * [GameGymEnv] only; the service casts when it needs them.
 */
interface GymEnv {

    /** True once the env reached a terminal state (game over, or deck finalized). */
    val isTerminal: Boolean

    /**
     * Current observation without advancing. [revealAll] is honoured by game envs
     * (unmask opponent hand/libraries) and ignored by deckbuild envs, which have no
     * hidden information. Passing null uses the env's configured default.
     */
    fun observe(revealAll: Boolean? = null): ObservationResult

    /**
     * Advance by the action with [actionId] from the most recent observation.
     * @throws IllegalArgumentException if the ID is stale / not valid this step.
     */
    fun step(actionId: Int): ObservationResult

    /** Branch this env. Children diverge independently from the next [step] on. */
    fun fork(): GymEnv
}
