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
import com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect
import kotlin.reflect.KClass

/**
 * Executor for ExchangeControlEffect.
 *
 * Exchanges control of two objects (CR 701.12b) — each a permanent on the battlefield or a spell on
 * the stack. A permanent changes hands through a Layer.CONTROL floating effect; a spell through a
 * rewrite of its stack object's controller.
 */
class ExchangeControlExecutor : EffectExecutor<ExchangeControlEffect> {

    override val effectType: KClass<ExchangeControlEffect> = ExchangeControlEffect::class

    override fun execute(
        state: GameState,
        effect: ExchangeControlEffect,
        context: EffectContext
    ): EffectResult {
        val target1Id = context.resolveTarget(effect.target1)
            ?: return EffectResult.error(state, "No valid first target for exchange")

        val target2Id = context.resolveTarget(effect.target2)
            ?: return EffectResult.error(state, "No valid second target for exchange")

        val container1 = state.getEntity(target1Id)
            ?: return EffectResult.error(state, "First target no longer exists")

        val container2 = state.getEntity(target2Id)
            ?: return EffectResult.error(state, "Second target no longer exists")

        val card1 = container1.get<CardComponent>()
            ?: return EffectResult.error(state, "First target is not a card")

        val card2 = container2.get<CardComponent>()
            ?: return EffectResult.error(state, "Second target is not a card")

        // CR 701.12a: if the entire exchange can't be completed, none of it happens. Each side is a
        // permanent on the battlefield or a spell on the stack (Perplexing Chimera: "exchange control
        // of this creature and that spell"); a side that has left its zone — the creature gone, or
        // the spell resolved or countered before the trigger resolves — has no control to exchange.
        val spell1 = stackSpell(state, target1Id)
        val spell2 = stackSpell(state, target2Id)
        val battlefield = state.getBattlefield()
        if ((spell1 == null && target1Id !in battlefield) || (spell2 == null && target2Id !in battlefield)) {
            return EffectResult.success(state)
        }

        // A spell's controller is its stack object's casterId; a permanent's is projected (so
        // floating-effect-based control changes are respected).
        val controller1 = spell1?.casterId
            ?: state.projectedState.getController(target1Id)
            ?: container1.get<ControllerComponent>()?.playerId
            ?: return EffectResult.error(state, "First target has no controller")

        val controller2 = spell2?.casterId
            ?: state.projectedState.getController(target2Id)
            ?: container2.get<ControllerComponent>()?.playerId
            ?: return EffectResult.error(state, "Second target has no controller")

        // If both creatures already have the same controller, no-op
        if (controller1 == controller2) return EffectResult.success(state)

        // "Other players can't gain control of it" (Guardian Beast): an exchange would hand each
        // permanent to a different player, so if either side can't be gained control of, the whole
        // exchange fails to happen.
        if (state.projectedState.hasKeyword(target1Id, AbilityFlag.CANT_GAIN_CONTROL) ||
            state.projectedState.hasKeyword(target2Id, AbilityFlag.CANT_GAIN_CONTROL)
        ) {
            return EffectResult.success(state)
        }

        // Capture the base timestamp before threading state so that +1 is stable
        val baseTimestamp = state.timestamp

        var newState = giveControl(state, target1Id, spell1, controller2, context, baseTimestamp)
        newState = giveControl(newState, target2Id, spell2, controller1, context, baseTimestamp + 1)

        val events = listOf(
            ControlChangedEvent(
                permanentId = target1Id,
                permanentName = card1.name,
                oldControllerId = controller1,
                newControllerId = controller2
            ),
            ControlChangedEvent(
                permanentId = target2Id,
                permanentName = card2.name,
                oldControllerId = controller2,
                newControllerId = controller1
            )
        )

        return EffectResult.success(newState, events)
    }

    private fun stackSpell(state: GameState, entityId: EntityId): SpellOnStackComponent? =
        if (entityId in state.stack) state.getEntity(entityId)?.get<SpellOnStackComponent>() else null

    /**
     * Hand one side of the exchange to [newControllerId]. A permanent gets a Layer.CONTROL floating
     * effect and summoning sickness (Rule 302.6: it hasn't been under its new controller's control
     * continuously since their most recent turn began). A spell's controller is not a layer
     * characteristic — continuous effects never touch objects on the stack — so it is rewritten on
     * the stack object for as long as the spell stays there, exactly as "gain control of target
     * spell" does ([GainControlExecutor.setSpellController]).
     */
    private fun giveControl(
        state: GameState,
        entityId: EntityId,
        spell: SpellOnStackComponent?,
        newControllerId: EntityId,
        context: EffectContext,
        timestamp: Long
    ): GameState {
        if (spell != null) return GainControlExecutor.setSpellController(state, entityId, spell, newControllerId)
        return state.addFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(newControllerId),
            affectedEntities = setOf(entityId),
            duration = Duration.Permanent,
            context = context,
            timestamp = timestamp
        )
            .updateEntity(entityId) { it.with(SummoningSicknessComponent) }
            .let { clearRingBearerOnControlChange(it, entityId, newControllerId) }
    }
}
