package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.sba.StateBasedActionRegistry
import com.wingedsheep.engine.mechanics.sba.creature.CreatureSbaModule
import com.wingedsheep.engine.mechanics.sba.game.GameSbaModule
import com.wingedsheep.engine.mechanics.sba.permanent.PermanentSbaModule
import com.wingedsheep.engine.mechanics.sba.player.PlayerSbaModule
import com.wingedsheep.engine.mechanics.sba.zone.ZoneSbaModule
import com.wingedsheep.engine.state.GameState

/**
 * Checks and applies state-based actions (Rule 704).
 *
 * State-based actions are game actions that happen automatically whenever
 * certain conditions are true. They don't use the stack.
 *
 * SBAs are checked:
 * - After a spell/ability resolves
 * - Before a player gets priority
 * - During cleanup step
 *
 * SBAs are checked repeatedly until none apply.
 *
 * Individual checks are registered via [StateBasedActionRegistry] and run in
 * [com.wingedsheep.engine.mechanics.sba.SbaOrder] order.
 */
class StateBasedActionChecker(
    private val registry: StateBasedActionRegistry
) {
    /**
     * Backward-compatible constructor used by existing call sites.
     */
    constructor(
        decisionHandler: DecisionHandler = DecisionHandler(),
        cardRegistry: com.wingedsheep.engine.registry.CardRegistry? = null
    ) : this(buildDefaultRegistry(decisionHandler, cardRegistry))

    /**
     * Check and apply all state-based actions until none apply.
     * Returns the new state and all events that occurred.
     */
    fun checkAndApply(state: GameState): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        // Keep checking until no SBAs apply
        var actionsApplied: Boolean
        do {
            val result = checkOnce(currentState)

            // If an SBA needs player input (e.g., legend rule choice), return paused
            if (result.isPaused) {
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            actionsApplied = result.events.isNotEmpty()
            currentState = result.newState
            allEvents.addAll(result.events)
        } while (actionsApplied)

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Check state-based actions once by running all registered checks in order.
     */
    private fun checkOnce(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (check in registry.allChecks()) {
            val result = check.check(newState)

            if (result.isPaused) {
                // Return paused with events accumulated so far + this check's events
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    events + result.events
                )
            }

            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    companion object {
        fun buildDefaultRegistry(
            decisionHandler: DecisionHandler = DecisionHandler(),
            cardRegistry: com.wingedsheep.engine.registry.CardRegistry? = null
        ): StateBasedActionRegistry {
            val registry = StateBasedActionRegistry()
            registry.registerModule(PlayerSbaModule())
            registry.registerModule(CreatureSbaModule())
            registry.registerModule(PermanentSbaModule(decisionHandler, cardRegistry))
            registry.registerModule(ZoneSbaModule())
            registry.registerModule(GameSbaModule())
            return registry
        }
    }
}
