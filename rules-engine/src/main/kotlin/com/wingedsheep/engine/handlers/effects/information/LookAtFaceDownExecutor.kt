package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LookedAtCardsEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolvePlayerTarget
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownLookScope
import com.wingedsheep.sdk.scripting.effects.LookAtFaceDownEffect
import kotlin.reflect.KClass

/**
 * Executor for LookAtFaceDownEffect.
 *
 * When scope is SINGLE_TARGET: marks a single face-down creature as revealed to the controller.
 * When scope is ALL_CONTROLLED_BY_TARGET_PLAYER: marks all face-down creatures controlled by
 * the target player as revealed to the controller.
 */
class LookAtFaceDownExecutor : EffectExecutor<LookAtFaceDownEffect> {

    override val effectType: KClass<LookAtFaceDownEffect> = LookAtFaceDownEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtFaceDownEffect,
        context: EffectContext
    ): ExecutionResult {
        val viewingPlayerId = context.controllerId

        return when (effect.scope) {
            FaceDownLookScope.SINGLE_TARGET -> executeSingleTarget(state, effect, context, viewingPlayerId)
            FaceDownLookScope.ALL_CONTROLLED_BY_TARGET_PLAYER -> executeAllControlled(state, effect, context, viewingPlayerId)
        }
    }

    private fun executeSingleTarget(
        state: GameState,
        effect: LookAtFaceDownEffect,
        context: EffectContext,
        viewingPlayerId: com.wingedsheep.sdk.model.EntityId
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for look at face-down creature")

        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<RevealedToComponent>()
            if (existing != null) {
                container.with(existing.withPlayer(viewingPlayerId))
            } else {
                container.with(RevealedToComponent.to(viewingPlayerId))
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(LookedAtCardsEvent(viewingPlayerId, listOf(targetId), source = "Look at face-down creature"))
        )
    }

    private fun executeAllControlled(
        state: GameState,
        effect: LookAtFaceDownEffect,
        context: EffectContext,
        viewingPlayerId: com.wingedsheep.sdk.model.EntityId
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target player for look at all face-down creatures")

        val battlefield = state.controlledBattlefield(targetPlayerId)
        val faceDownCreatures = battlefield.filter { entityId ->
            val container = state.getEntity(entityId)
            container != null && container.has<FaceDownComponent>()
        }

        if (faceDownCreatures.isEmpty()) {
            return ExecutionResult.success(state)
        }

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
