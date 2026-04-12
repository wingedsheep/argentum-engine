package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import kotlin.reflect.KClass

/**
 * Executor for TurnFaceUpEffect.
 * "Turn target face-down creature face up."
 *
 * Removes FaceDownComponent from the target creature, turning it face up.
 * Unlike the morph special action (TurnFaceUpHandler), this does not require
 * payment of the morph cost — the spell effect simply flips the creature.
 */
class TurnFaceUpExecutor(
    cardRegistry: CardRegistry
) : EffectExecutor<TurnFaceUpEffect> {

    private val staticAbilityHandler = StaticAbilityHandler(cardRegistry)

    override val effectType: KClass<TurnFaceUpEffect> = TurnFaceUpEffect::class

    override fun execute(
        state: GameState,
        effect: TurnFaceUpEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for turn face up")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found")

        // Already face-up — nothing to do
        if (!container.has<FaceDownComponent>()) {
            return EffectResult.success(state)
        }

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId
        val cardName = container.get<CardComponent>()?.name ?: "Unknown"

        val newState = state.updateEntity(targetId) { c ->
            var updated = c.without<FaceDownComponent>()
            updated = staticAbilityHandler.addContinuousEffectComponent(updated)
            updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            updated
        }

        return EffectResult.success(
            newState,
            listOf(TurnFaceUpEvent(targetId, cardName, controllerId))
        )
    }
}
