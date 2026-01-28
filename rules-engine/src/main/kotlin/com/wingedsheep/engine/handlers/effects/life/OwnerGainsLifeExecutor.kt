package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.OwnerGainsLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for OwnerGainsLifeEffect.
 * "Its owner gains X life" - gives life to the owner of the targeted permanent.
 */
class OwnerGainsLifeExecutor : EffectExecutor<OwnerGainsLifeEffect> {

    override val effectType: KClass<OwnerGainsLifeEffect> = OwnerGainsLifeEffect::class

    override fun execute(
        state: GameState,
        effect: OwnerGainsLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get the first target from the context (the creature that was targeted by the spell)
        val targetId = context.targets.firstOrNull()?.let {
            when (it) {
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> it.entityId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> it.cardId
                else -> null
            }
        } ?: return ExecutionResult.success(state) // No target, effect fizzles gracefully

        // Find the owner of the targeted permanent
        val targetContainer = state.getEntity(targetId)
        val ownerId = targetContainer?.get<OwnerComponent>()?.playerId
            ?: targetContainer?.get<CardComponent>()?.ownerId
            ?: return ExecutionResult.success(state) // Can't determine owner, effect fizzles

        // Get the owner's current life total
        val currentLife = state.getEntity(ownerId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Owner has no life total")

        // Apply life gain
        val newLife = currentLife + effect.amount
        val newState = state.updateEntity(ownerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(ownerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        )
    }
}
