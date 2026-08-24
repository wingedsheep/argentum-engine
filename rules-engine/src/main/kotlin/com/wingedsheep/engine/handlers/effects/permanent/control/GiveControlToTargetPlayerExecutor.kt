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
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import kotlin.reflect.KClass

/**
 * Executor for GiveControlToTargetPlayerEffect.
 *
 * Gives control of a permanent to a targeted player (not the ability's controller).
 * Used by Custody Battle to give control of enchanted creature to a target opponent.
 */
class GiveControlToTargetPlayerExecutor : EffectExecutor<GiveControlToTargetPlayerEffect> {

    override val effectType: KClass<GiveControlToTargetPlayerEffect> = GiveControlToTargetPlayerEffect::class

    override fun execute(
        state: GameState,
        effect: GiveControlToTargetPlayerEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.permanent, state)
            ?: return EffectResult.error(state, "No valid permanent for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target permanent no longer exists")

        // CR 110.2a — only a permanent has a controller, so a target that has left the battlefield
        // between targeting and resolution simply isn't affected (CR 608.2b). Silently, and with no
        // ControlChangedEvent: `SuccessCriterion.ControlChanged` riders ("...gains control of target
        // permanent you control. If they do, you draw a card") read that event to decide whether the
        // control change really happened, so emitting one here would pay out for a permanent that is
        // sitting in a graveyard.
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        // State-aware overload: the new controller may be a relational / stored player reference
        // (Player.ChosenOpponent for Wishclaw Talisman's "an opponent gains control of this
        // artifact", Player.ControllerOf, …), none of which the stateless resolver can see.
        val newControllerId = context.resolvePlayerTarget(effect.newController, state)
            ?: return EffectResult.error(state, "No valid player target for control change")

        // Use projected controller so floating-effect-based control changes are respected
        val currentControllerId = state.projectedState.getController(targetId)
            ?: targetContainer.get<ControllerComponent>()?.playerId

        // Remove any previous Layer.CONTROL floating effects from the same source on the same target
        val filteredEffects = state.floatingEffects.filter { floating ->
            !(floating.sourceId == context.sourceId &&
              floating.effect.layer == Layer.CONTROL &&
              targetId in floating.effect.affectedEntities)
        }

        // If the controller already matches after removing old floating effects, no new effect needed
        // and no summoning sickness — control is not actually changing.
        val newState = if (currentControllerId == newControllerId) {
            state.copy(floatingEffects = filteredEffects)
        } else {
            val controlContext = context.copy(controllerId = newControllerId)
            // Rule 302.6: new controller hasn't had this permanent since their most recent turn began.
            state.copy(floatingEffects = filteredEffects)
                .addFloatingEffect(
                    layer = Layer.CONTROL,
                    modification = SerializableModification.ChangeController(newControllerId),
                    affectedEntities = setOf(targetId),
                    duration = effect.duration,
                    context = controlContext
                )
                .updateEntity(targetId) { it.with(SummoningSicknessComponent) }
                .let { clearRingBearerOnControlChange(it, targetId, newControllerId) }
        }

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
