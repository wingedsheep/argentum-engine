package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.OwnerGainsLifeEffect
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
    ): EffectResult {
        // Get the first target from the context (the creature that was targeted by the spell)
        val targetId = context.targets.firstOrNull()?.let {
            when (it) {
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> it.entityId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> it.cardId
                else -> null
            }
        } ?: return EffectResult.success(state) // No target, effect fizzles gracefully

        // Find the owner of the targeted permanent
        val targetContainer = state.getEntity(targetId)
        val ownerId = targetContainer?.get<OwnerComponent>()?.playerId
            ?: targetContainer?.get<CardComponent>()?.ownerId
            ?: return EffectResult.success(state) // Can't determine owner, effect fizzles

        // Owner must have a life total (presence check; the value is the team's shared total).
        if (state.getEntity(ownerId)?.get<LifeTotalComponent>() == null) {
            return EffectResult.error(state, "Owner has no life total")
        }

        // PreventLifeGain / ModifyLifeGain replacements are applied by the shared primitive.
        val (newState, event) = DamageUtils.gainLife(state, ownerId, effect.amount)
        return EffectResult.success(newState, listOfNotNull(event))
    }
}
