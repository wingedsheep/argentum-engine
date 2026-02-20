package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LookedAtCardsEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.scripting.effects.LookAtAllFaceDownCreaturesEffect
import kotlin.reflect.KClass

/**
 * Executor for LookAtAllFaceDownCreaturesEffect.
 * "Look at any face-down creatures target player controls"
 *
 * Finds all face-down creatures controlled by the target player on the battlefield
 * and marks them as revealed to the controller of the ability.
 */
class LookAtAllFaceDownCreaturesExecutor : EffectExecutor<LookAtAllFaceDownCreaturesEffect> {

    override val effectType: KClass<LookAtAllFaceDownCreaturesEffect> = LookAtAllFaceDownCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtAllFaceDownCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target player for look at all face-down creatures")

        val viewingPlayerId = context.controllerId

        // Find all face-down creatures on the target player's battlefield
        // getBattlefield(playerId) already scopes to that player's zone
        val battlefield = state.getBattlefield(targetPlayerId)
        val faceDownCreatures = battlefield.filter { entityId ->
            val container = state.getEntity(entityId)
            container != null && container.has<FaceDownComponent>()
        }

        if (faceDownCreatures.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Mark each face-down creature as revealed to the viewing player
        var newState = state
        for (creatureId in faceDownCreatures) {
            newState = newState.updateEntity(creatureId) { container ->
                val existing = container.get<RevealedToComponent>()
                if (existing != null) {
                    container.with(existing.withPlayer(viewingPlayerId))
                } else {
                    container.with(RevealedToComponent.to(viewingPlayerId))
                }
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(LookedAtCardsEvent(viewingPlayerId, faceDownCreatures, source = "Look at face-down creatures"))
        )
    }
}
