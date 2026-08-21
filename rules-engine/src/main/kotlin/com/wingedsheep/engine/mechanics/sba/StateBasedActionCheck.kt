package com.wingedsheep.engine.mechanics.sba

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.GameState

/**
 * A single state-based action check (Rule 704).
 *
 * SBAs differ from actions/effects in that they don't dispatch by type -
 * they all run in ordered sequence every time SBAs are checked.
 */
interface StateBasedActionCheck {
    /** Descriptive name, e.g. "704.5a Player Life Loss" */
    val name: String

    /** Ordering constant - lower runs first. Use [SbaOrder] constants. */
    val order: Int

    /** Check and apply this SBA. Returns success with events if changes were made. */
    fun check(state: GameState): ExecutionResult

    /**
     * Check and apply this SBA, told what the game looked like when the pass began.
     *
     * CR 704.3 performs every applicable state-based action *simultaneously as a single event*,
     * but the checks here run in [SbaOrder] sequence and each moves permanents one at a time.
     * A check that needs to see the battlefield as it stood before any of that batch left —
     * a "would die → exile it instead" shield that is itself dying, say — overrides this and
     * reads [passStartState]. Everything else keeps the single-argument form.
     */
    fun check(state: GameState, passStartState: GameState): ExecutionResult = check(state)
}

/**
 * A module that provides a group of related [StateBasedActionCheck]s.
 */
interface StateBasedActionModule {
    fun checks(): List<StateBasedActionCheck>
}

/**
 * Registry of all state-based action checks, sorted by order.
 */
class StateBasedActionRegistry {
    private val checks = mutableListOf<StateBasedActionCheck>()

    fun registerModule(module: StateBasedActionModule) {
        checks.addAll(module.checks())
        checks.sortBy { it.order }
    }

    fun allChecks(): List<StateBasedActionCheck> = checks.toList()
}
