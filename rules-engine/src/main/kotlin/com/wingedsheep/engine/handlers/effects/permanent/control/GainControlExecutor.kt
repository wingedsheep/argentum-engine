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
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import kotlin.reflect.KClass

/**
 * Executor for GainControlEffect.
 *
 * Gains control of target permanent — or target spell on the stack — for the controller of the
 * spell/ability.
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

        targetContainer.get<SpellOnStackComponent>()?.let { spell ->
            return gainControlOfSpell(state, targetId, spell, cardComponent, context.controllerId)
        }

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
            if (effect.duration == Duration.WhileSourceAttachedToAffected) {
                context.copy(sourceId = context.triggeringEntityId ?: context.sourceId)
            } else context

        // Remove any previous Layer.CONTROL floating effects from the same source on the same target
        val filteredEffects = state.floatingEffects.filter { floating ->
            !(floating.sourceId == floatingContext.sourceId &&
              floating.effect.layer == Layer.CONTROL &&
              targetId in floating.effect.affectedEntities)
        }

        val shouldStampSummoningSickness = effect.duration !is Duration.WhileSourceTappedAndAffectedPowerAtMostSource

        val stateWithControlEffect = state.copy(floatingEffects = filteredEffects)
            .addFloatingEffect(
                layer = Layer.CONTROL,
                modification = SerializableModification.ChangeController(newControllerId),
                affectedEntities = setOf(targetId),
                duration = effect.duration,
                context = floatingContext
            )

        // Ordinary control changes make the creature unable to attack/tap this turn. Old Man of
        // the Sea's duration is the long-lived exception in this engine: keeping the source tapped
        // preserves the stolen creature as usable under the new controller.
        val stateWithSickness = if (shouldStampSummoningSickness) {
            stateWithControlEffect.updateEntity(targetId) { it.with(SummoningSicknessComponent) }
        } else {
            stateWithControlEffect
        }

        val newState = stateWithSickness
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

    /**
     * "Gain control of target spell" (Invert Polarity). A spell's controller is not a layer
     * characteristic — continuous effects never touch objects on the stack — so the change is a
     * direct rewrite of the stack object: [SpellOnStackComponent.casterId] is what every
     * resolution, targeting and "you" read uses as the spell's controller (copies, which are
     * never cast, already store their controller there). It lasts until the spell leaves the
     * stack, so [GainControlEffect.duration] has nothing to bound; a resolving permanent spell
     * enters under the new controller because permanent entry reads the same field.
     */
    private fun gainControlOfSpell(
        state: GameState,
        spellId: EntityId,
        spell: SpellOnStackComponent,
        cardComponent: CardComponent,
        newControllerId: EntityId
    ): EffectResult {
        val oldControllerId = spell.casterId
        if (oldControllerId == newControllerId) return EffectResult.success(state)
        val newState = state.updateEntity(spellId) { container ->
            val updated = container.with(spell.copy(casterId = newControllerId))
            if (updated.has<ControllerComponent>()) updated.with(ControllerComponent(newControllerId)) else updated
        }
        return EffectResult.success(
            newState,
            listOf(ControlChangedEvent(spellId, cardComponent.name, oldControllerId, newControllerId))
        )
    }
}
