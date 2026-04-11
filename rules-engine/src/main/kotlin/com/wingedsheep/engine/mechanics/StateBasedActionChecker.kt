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
        cardRegistry: com.wingedsheep.engine.registry.CardRegistry
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
        var iterations = 0
        var lastIterationEvents: List<GameEvent> = emptyList()
        do {
            if (iterations >= MAX_SBA_ITERATIONS) {
                // SBAs did not stabilize. Per MTG rule 104.4c, an unbreakable infinite
                // loop ends the game in a draw — that's the correct outcome whether this
                // is a legitimate combo (e.g. Worldgorger Dragon shenanigans) or a bug
                // (a misbehaving replacement effect / card script). We still log to
                // stderr so developers can distinguish the two cases after the fact.
                System.err.println(
                    "StateBasedActionChecker: SBAs did not stabilize after " +
                            "$MAX_SBA_ITERATIONS iterations — ending game as a draw " +
                            "(rule 104.4c). Last iteration events: $lastIterationEvents"
                )
                val drawnState = currentState.copy(gameOver = true, winnerId = null)
                val drawEvent = GameEndedEvent(winnerId = null, reason = GameEndReason.INFINITE_LOOP)
                return ExecutionResult.success(drawnState, allEvents + drawEvent)
            }

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
            lastIterationEvents = result.events
            currentState = result.newState
            allEvents.addAll(result.events)
            iterations++
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
        /**
         * Maximum number of SBA iterations before the checker bails out. SBAs normally
         * stabilize in 1–3 passes; exceeding this cap means either a bug (replacement
         * effect / trigger regenerating events forever) or a legitimate MTG infinite
         * loop (rule 726). Either way the engine must not hang.
         */
        const val MAX_SBA_ITERATIONS = 1000

        fun buildDefaultRegistry(
            decisionHandler: DecisionHandler = DecisionHandler(),
            cardRegistry: com.wingedsheep.engine.registry.CardRegistry
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
