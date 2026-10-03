package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.BecameMonstrousEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.BecomeMonstrousEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeMonstrousEffect] — the designation half of monstrosity (CR 701.37a).
 * Stamps the [MonstrousComponent] marker and emits a [BecameMonstrousEvent].
 *
 * "Monstrous" is a designation (CR 701.37b), not an ability and not a copiable value: nothing about
 * the permanent's characteristics changes here. Payoffs read the marker back through
 * `Conditions.SourceIsMonstrous`.
 *
 * Making an already-monstrous permanent monstrous is a no-op that emits no event, so a "when this
 * becomes monstrous" trigger fires only once per permanent.
 */
class BecomeMonstrousExecutor : EffectExecutor<BecomeMonstrousEffect> {

    override val effectType: KClass<BecomeMonstrousEffect> = BecomeMonstrousEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeMonstrousEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // CR 701.37b — only permanents can be or become monstrous. A creature that left the
        // battlefield in response to its own monstrosity ability simply does nothing.
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val container = state.getEntity(targetId) ?: return EffectResult.success(state)
        if (container.has<MonstrousComponent>()) {
            return EffectResult.success(state)
        }

        val name = container.get<CardComponent>()?.name ?: "Unknown"
        // Read from the projection so a creature under someone else's control credits that player.
        val controller = state.projectedState.getController(targetId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: context.controllerId
        val newState = state.updateEntity(targetId) { it.with(MonstrousComponent) }

        return EffectResult.success(newState, listOf(BecameMonstrousEvent(targetId, name, controller)))
    }
}
