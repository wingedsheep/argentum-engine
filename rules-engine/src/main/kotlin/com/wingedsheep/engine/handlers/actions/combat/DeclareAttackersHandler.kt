package com.wingedsheep.engine.handlers.actions.combat

import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import kotlin.reflect.KClass

/**
 * Handler for the DeclareAttackers action.
 *
 * Delegates to CombatManager for the actual attack declaration,
 * then processes any attack triggers.
 */
class DeclareAttackersHandler(
    private val combatManager: CombatManager,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<DeclareAttackers> {
    override val actionType: KClass<DeclareAttackers> = DeclareAttackers::class

    override fun validate(state: GameState, action: DeclareAttackers): String? {
        // CR 805.10a/b — the whole active team is the attacking team, so either teammate may make
        // the team's one combined attack declaration.
        if (!state.isActiveTurnFor(action.playerId)) {
            return "You can only declare attackers on your turn"
        }
        if (state.step != Step.DECLARE_ATTACKERS) {
            return "You can only declare attackers during the declare attackers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    override fun execute(state: GameState, action: DeclareAttackers): ExecutionResult {
        val result = combatManager.declareAttackers(state, action.playerId, action.attackers, action.bands)

        if (!result.isSuccess) {
            return result
        }

        // Detect and process attack triggers (e.g., "when this creature attacks")
        val triggers = triggerDetector.detectTriggers(result.newState, result.events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(result.newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    result.events + triggerResult.events
                )
            }

            // CR 603.3b cascade — an ability that triggers off *another ability being put on the
            // stack* (Firebender Ascension: "a creature you control attacking causes a triggered
            // ability of that creature to trigger") isn't seen by the single detection pass above,
            // because the attack triggers' `AbilityTriggeredEvent`s are emitted while processing
            // them, not by combat itself. Re-detect from the wave of *attack-caused*
            // `AbilityTriggeredEvent`s and put those reactions on the stack too. Scoping the wave to
            // `causedByAttack` events is what makes this terminate: a cascade-placed ability is never
            // itself attack-caused, so it produces no further wave — no Grip-of-Chaos-style
            // self-loop (the reason global trigger detection stays single-pass). The guard is a
            // belt-and-suspenders bound.
            val cascadeResult = processAttackCascade(triggerResult.newState, triggerResult.events)
            if (cascadeResult.isPaused) {
                return ExecutionResult.paused(
                    cascadeResult.state,
                    cascadeResult.pendingDecision!!,
                    result.events + triggerResult.events + cascadeResult.events
                )
            }

            return ExecutionResult.success(
                cascadeResult.newState,
                result.events + triggerResult.events + cascadeResult.events
            )
        }

        return result
    }

    /**
     * Re-detect and place abilities that trigger off an attack-caused triggered ability going on the
     * stack (see the call site). [priorEvents] is the wave emitted by the initial attack-trigger
     * processing; each round scans only the `causedByAttack` `AbilityTriggeredEvent`s in the prior
     * wave, so it naturally converges (cascade placements aren't attack-caused). Returns the final
     * state plus every event emitted across the cascade rounds.
     */
    private fun processAttackCascade(state: GameState, priorEvents: List<GameEvent>): ExecutionResult {
        var workingState = state
        val emitted = mutableListOf<GameEvent>()
        var wave = priorEvents.filterIsInstance<AbilityTriggeredEvent>().filter { it.causedByAttack }
        var guard = 0
        while (wave.isNotEmpty() && guard < CASCADE_GUARD) {
            val cascadeTriggers = triggerDetector.detectTriggers(workingState, wave)
            if (cascadeTriggers.isEmpty()) break
            val roundResult = triggerProcessor.processTriggers(workingState, cascadeTriggers)
            emitted.addAll(roundResult.events)
            if (roundResult.isPaused) {
                return ExecutionResult.paused(roundResult.state, roundResult.pendingDecision!!, emitted)
            }
            workingState = roundResult.newState
            wave = roundResult.events.filterIsInstance<AbilityTriggeredEvent>().filter { it.causedByAttack }
            guard++
        }
        return ExecutionResult.success(workingState, emitted)
    }

    companion object {
        /** Hard bound on cascade rounds; convergence is by the `causedByAttack` wave filter. */
        private const val CASCADE_GUARD = 16

        fun create(services: EngineServices): DeclareAttackersHandler {
            return DeclareAttackersHandler(
                services.combatManager,
                services.triggerDetector,
                services.triggerProcessor
            )
        }
    }
}
