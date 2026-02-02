package com.wingedsheep.engine.handlers.actions.combat

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.actions.ActionContext
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
        if (state.activePlayerId != action.playerId) {
            return "You can only declare attackers on your turn"
        }
        if (state.step != Step.DECLARE_ATTACKERS) {
            return "You can only declare attackers during the declare attackers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    override fun execute(state: GameState, action: DeclareAttackers): ExecutionResult {
        val result = combatManager.declareAttackers(state, action.playerId, action.attackers)

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

            return ExecutionResult.success(
                triggerResult.newState,
                result.events + triggerResult.events
            )
        }

        return result
    }

    companion object {
        fun create(context: ActionContext): DeclareAttackersHandler {
            return DeclareAttackersHandler(
                context.combatManager,
                context.triggerDetector,
                context.triggerProcessor
            )
        }
    }
}
