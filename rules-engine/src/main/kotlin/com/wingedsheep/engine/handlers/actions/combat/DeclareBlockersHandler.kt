package com.wingedsheep.engine.handlers.actions.combat

import com.wingedsheep.engine.core.DeclareBlockers
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
 * Handler for the DeclareBlockers action.
 *
 * Delegates to CombatManager for the actual block declaration,
 * then processes any block triggers.
 */
class DeclareBlockersHandler(
    private val combatManager: CombatManager,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<DeclareBlockers> {
    override val actionType: KClass<DeclareBlockers> = DeclareBlockers::class

    override fun validate(state: GameState, action: DeclareBlockers): String? {
        if (state.activePlayerId == action.playerId) {
            return "You cannot declare blockers on your turn"
        }
        if (state.step != Step.DECLARE_BLOCKERS) {
            return "You can only declare blockers during the declare blockers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    override fun execute(state: GameState, action: DeclareBlockers): ExecutionResult {
        val result = combatManager.declareBlockers(state, action.playerId, action.blockers)

        if (!result.isSuccess) {
            return result
        }

        // Detect and process block triggers (e.g., "when this creature blocks")
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
        fun create(context: ActionContext): DeclareBlockersHandler {
            return DeclareBlockersHandler(
                context.combatManager,
                context.triggerDetector,
                context.triggerProcessor
            )
        }
    }
}
