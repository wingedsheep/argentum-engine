package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.BecameSaddledEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BecomeSaddledEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeSaddledEffect] — the resolving half of a Saddle ability (CR 702.171a).
 * Stamps the [SaddledComponent] marker on the target permanent and emits a [BecameSaddledEvent].
 *
 * "Saddled" is just a marker (CR 702.171b): no P/T, type, or ability change, unlike Crew's
 * BecomeCreature. Re-saddling an already-saddled permanent is legal and harmless — the marker is
 * idempotent — so we re-apply unconditionally rather than checking first.
 */
class BecomeSaddledExecutor : EffectExecutor<BecomeSaddledEffect> {

    override val effectType: KClass<BecomeSaddledEffect> = BecomeSaddledEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeSaddledEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // Only permanents on the battlefield can be saddled (CR 702.171b). If the source left
        // play before the ability resolved, the effect simply does nothing.
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val name = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Unknown"
        // First time saddled this turn iff it wasn't already saddled. SaddledComponent is set the
        // first time and cleared only at the cleanup step (CR 702.171b), so a re-saddle in the same
        // turn reports false — exactly the "for the first time each turn" intervening-if semantics.
        val firstThisTurn = state.getEntity(targetId)?.has<SaddledComponent>() != true
        val newState = state.updateEntity(targetId) { it.with(SaddledComponent) }

        return EffectResult.success(newState, listOf(BecameSaddledEvent(targetId, name, firstThisTurn)))
    }
}
