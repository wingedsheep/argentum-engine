package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LookedAtCardsEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for LookAtFaceDownCreatureEffect.
 * "Look at target face-down creature"
 *
 * Marks the face-down creature as revealed to the controller of the ability,
 * allowing them to see its real identity.
 */
class LookAtFaceDownCreatureExecutor : EffectExecutor<LookAtFaceDownCreatureEffect> {

    override val effectType: KClass<LookAtFaceDownCreatureEffect> = LookAtFaceDownCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtFaceDownCreatureEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for look at face-down creature")

        val viewingPlayerId = context.controllerId

        // Mark the creature as revealed to the viewing player
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
}
