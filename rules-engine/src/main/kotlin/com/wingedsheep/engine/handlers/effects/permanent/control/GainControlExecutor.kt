package com.wingedsheep.engine.handlers.effects.permanent.control

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import kotlin.reflect.KClass

/**
 * Executor for GainControlEffect.
 *
 * Gains control of target permanent for the controller of the spell/ability.
 */
class GainControlExecutor : EffectExecutor<GainControlEffect> {

    override val effectType: KClass<GainControlEffect> = GainControlEffect::class

    override fun execute(
        state: GameState,
        effect: GainControlEffect,
        context: EffectContext
    ): EffectResult {
        // Use the state-aware overload so attachment-relative targets (e.g.
        // EnchantedPermanent for an aura on a land) resolve via AttachedToComponent.
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target permanent no longer exists")

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        // "Other players can't gain control of it" (Guardian Beast): a different player can't take
        // control. The controller keeping control of their own permanent is a no-op anyway.
        if (state.projectedState.hasKeyword(targetId, AbilityFlag.CANT_GAIN_CONTROL) &&
            state.projectedState.getController(targetId) != context.controllerId
        ) {
            return EffectResult.success(state)
        }

        val newControllerId = context.controllerId

        // Use projected controller so floating-effect-based control changes are respected
        val currentControllerId = state.projectedState.getController(targetId)
            ?: targetContainer.get<ControllerComponent>()?.playerId
        if (currentControllerId == newControllerId) return EffectResult.success(state)

        // "for as long as that Aura is attached to it" (Eriette): the control effect's duration
        // tracks the *attachment* (the Aura/Equipment), not the card whose ability granted control.
        // Source the floating effect from the triggering attachment so
        // [Duration.WhileSourceAttachedToAffected] reads the right attachment's AttachedToComponent.
        val floatingContext =
            if (effect.duration == com.wingedsheep.sdk.scripting.Duration.WhileSourceAttachedToAffected) {
                context.copy(sourceId = context.triggeringEntityId ?: context.sourceId)
            } else context

        // Remove any previous Layer.CONTROL floating effects from the same source on the same target
        val filteredEffects = state.floatingEffects.filter { floating ->
            !(floating.sourceId == floatingContext.sourceId &&
              floating.effect.layer == Layer.CONTROL &&
              targetId in floating.effect.affectedEntities)
        }

        // Rule 302.6: new controller hasn't had this permanent since their most recent turn began.
        val newState = state.copy(floatingEffects = filteredEffects)
            .addFloatingEffect(
                layer = Layer.CONTROL,
                modification = SerializableModification.ChangeController(newControllerId),
                affectedEntities = setOf(targetId),
                duration = effect.duration,
                context = floatingContext
            )
            .updateEntity(targetId) { it.with(SummoningSicknessComponent) }
            .let { clearRingBearerOnControlChange(it, targetId, newControllerId) }

        val events = listOf(
            ControlChangedEvent(
                permanentId = targetId,
                permanentName = cardComponent.name,
                oldControllerId = currentControllerId ?: context.controllerId,
                newControllerId = newControllerId
            )
        )

        return EffectResult.success(newState, events)
    }
}
