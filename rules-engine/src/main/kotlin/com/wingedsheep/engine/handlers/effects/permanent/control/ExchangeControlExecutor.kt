package com.wingedsheep.engine.handlers.effects.permanent.control

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect
import kotlin.reflect.KClass

/**
 * Executor for ExchangeControlEffect.
 *
 * Exchanges control of two target creatures by creating two Layer.CONTROL
 * floating effects — one giving each creature to the other's controller.
 */
class ExchangeControlExecutor : EffectExecutor<ExchangeControlEffect> {

    override val effectType: KClass<ExchangeControlEffect> = ExchangeControlEffect::class

    override fun execute(
        state: GameState,
        effect: ExchangeControlEffect,
        context: EffectContext
    ): ExecutionResult {
        val target1Id = context.resolveTarget(effect.target1)
            ?: return ExecutionResult.error(state, "No valid first target for exchange")

        val target2Id = context.resolveTarget(effect.target2)
            ?: return ExecutionResult.error(state, "No valid second target for exchange")

        val container1 = state.getEntity(target1Id)
            ?: return ExecutionResult.error(state, "First target no longer exists")

        val container2 = state.getEntity(target2Id)
            ?: return ExecutionResult.error(state, "Second target no longer exists")

        val card1 = container1.get<CardComponent>()
            ?: return ExecutionResult.error(state, "First target is not a card")

        val card2 = container2.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Second target is not a card")

        val controller1 = container1.get<ControllerComponent>()?.playerId
            ?: return ExecutionResult.error(state, "First target has no controller")

        val controller2 = container2.get<ControllerComponent>()?.playerId
            ?: return ExecutionResult.error(state, "Second target has no controller")

        // If both creatures already have the same controller, no-op
        if (controller1 == controller2) return ExecutionResult.success(state)

        // Create floating effect: target1 gets controller2
        val floating1 = state.createFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(controller2),
            affectedEntities = setOf(target1Id),
            duration = Duration.Permanent,
            context = context
        )

        // Create floating effect: target2 gets controller1.
        // +1 preserves deterministic ordering between the two effects within Layer.CONTROL
        // (they affect different entities, so this is cosmetic, but matches the prior behavior).
        val floating2 = state.createFloatingEffect(
            layer = Layer.CONTROL,
            modification = SerializableModification.ChangeController(controller1),
            affectedEntities = setOf(target2Id),
            duration = Duration.Permanent,
            context = context,
            timestamp = state.timestamp + 1
        )

        val newState = state.addFloatingEffects(listOf(floating1, floating2))

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

        return ExecutionResult.success(newState, events)
    }
}
