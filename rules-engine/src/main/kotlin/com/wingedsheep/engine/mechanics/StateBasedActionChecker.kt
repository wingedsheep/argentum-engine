package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.ReplacementRiders
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
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
    private val registry: StateBasedActionRegistry,
    /**
     * Runs the rest of a replacement effect an SBA applied (see [ReplacementRiders]) — a lethal-damage
     * destruction replaced by "instead sacrifice this Aura and that land gains indestructible"
     * (Crackling Emergence). It is part of the same event, so it runs before the next pass looks.
     */
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)? = null
) {
    /**
     * Backward-compatible constructor used by existing call sites.
     */
    constructor(
        zones: ZoneTransitionService,
        decisionHandler: DecisionHandler = DecisionHandler(),
        cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
        /**
         * Runs the final part of a resolution abandoned when a player leaves —
         * [com.wingedsheep.engine.core.EngineServices.finishAbandonedResolution]. Required: see
         * [com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameProcessor.process].
         */
        finishAbandonedResolution: (GameState) -> ExecutionResult,
        effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)? = null
    ) : this(buildDefaultRegistry(zones, decisionHandler, cardRegistry, finishAbandonedResolution), effectExecutor)

    /**
     * Check and apply all state-based actions until none apply.
     * Returns the new state and all events that occurred.
     */
    fun checkAndApply(
        state: GameState,
        pendingTriggerSources: Set<com.wingedsheep.engine.state.ObjectRef> = emptySet()
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        // The rest of a replacement queued by the event that led here (combat damage applied by the
        // turn manager, which isn't an effect) is part of that event: it runs before any SBA sees the
        // result — an Enduring Angel dealt lethal damage in the same combat still transforms.
        val pendingRiders = runReplacementRiders(ExecutionResult.success(currentState))
        if (pendingRiders.outcome !is Outcome.Done) return pendingRiders
        currentState = pendingRiders.state
        allEvents.addAll(pendingRiders.events)

        // Keep checking until no SBAs apply
        var actionsApplied: Boolean
        var iterations = 0
        var lastIterationEvents: List<GameEvent> = emptyList()
        do {
            if (iterations >= MAX_SBA_ITERATIONS) {
                // SBAs did not stabilize. Per MTG rule 104.4b, an unbreakable infinite
                // loop ends the game in a draw — that's the correct outcome whether this
                // is a legitimate combo (e.g. Worldgorger Dragon shenanigans) or a bug
                // (a misbehaving replacement effect / card script). We still log to
                // stderr so developers can distinguish the two cases after the fact.
                System.err.println(
                    "StateBasedActionChecker: SBAs did not stabilize after " +
                            "$MAX_SBA_ITERATIONS iterations — ending game as a draw " +
                            "(rule 104.4b). Last iteration events: $lastIterationEvents"
                )
                val drawnState = currentState.copy(gameOver = true, winnerId = null)
                val drawEvent = GameEndedEvent(winnerId = null, reason = GameEndReason.INFINITE_LOOP)
                return ExecutionResult.success(drawnState, allEvents + drawEvent)
            }

            val result = checkOnce(currentState, pendingTriggerSources)

            // If an SBA needs player input (e.g., legend rule choice), return paused; if one was
            // rejected (an engine error), return that.
            if (result.outcome !is Outcome.Done) {
                return result.copy(events = allEvents + result.events)
            }

            val riders = runReplacementRiders(result)
            if (riders.outcome !is Outcome.Done) {
                return riders.copy(events = allEvents + riders.events)
            }

            actionsApplied = riders.events.isNotEmpty()
            lastIterationEvents = riders.events
            currentState = riders.newState
            allEvents.addAll(riders.events)
            iterations++
        } while (actionsApplied)

        return ExecutionResult.success(currentState, allEvents)
    }

    /** Run the replacement riders [pass] queued, so the next pass sees their result. */
    private fun runReplacementRiders(pass: ExecutionResult): ExecutionResult {
        val executor = effectExecutor ?: return pass
        if (pass.state.pendingReplacementRiders.isEmpty()) return pass
        val drained = ReplacementRiders.drain(pass.state, executor)
        return ExecutionResult(drained.state, pass.events + drained.events, drained.outcome)
    }

    /**
     * Check state-based actions once by running all registered checks in order.
     */
    private fun checkOnce(
        state: GameState,
        pendingTriggerSources: Set<com.wingedsheep.engine.state.ObjectRef>
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (check in registry.allChecks()) {
            // `state` is the pass-start snapshot: everything this pass performs is one
            // simultaneous event (CR 704.3), so a check that must see the battlefield as it
            // stood before the batch started gets it rather than reconstructing it.
            val result = check.check(newState, state, pendingTriggerSources)

            if (result.outcome !is Outcome.Done) {
                // Paused (or rejected): return it with the events accumulated so far + this check's
                return result.copy(events = events + result.events)
            }

            newState = result.newState
            events.addAll(result.events)
        }

        val result = ExecutionResult.success(newState, events)
        return GraveyardOrdering.finish(result)
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
            zones: ZoneTransitionService,
            decisionHandler: DecisionHandler = DecisionHandler(),
            cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
            finishAbandonedResolution: (GameState) -> ExecutionResult,
        ): StateBasedActionRegistry {
            val registry = StateBasedActionRegistry()
            registry.registerModule(PlayerSbaModule(zones, finishAbandonedResolution))
            registry.registerModule(CreatureSbaModule(zones))
            registry.registerModule(PermanentSbaModule(zones, decisionHandler, cardRegistry))
            registry.registerModule(ZoneSbaModule())
            registry.registerModule(GameSbaModule())
            return registry
        }
    }
}
