package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ActiveCounterPlacementModifier
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GrantCounterPlacementModifierEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantCounterPlacementModifierEffect].
 *
 * Installs a temporary, duration-scoped counter-placement modifier into
 * [GameState.activeCounterPlacementModifiers], stamped with the resolving ability's controller as
 * its `controllerId`. That controller becomes the "you" for both the recipient filter
 * ("a creature you control") and the placer gate (only the controller's own counter placements
 * receive the bonus) — exactly mirroring how the static `ModifyCounterPlacement` replacement is
 * controller-scoped to its source permanent's controller.
 *
 * The modifier is consulted from the single counter-placement chokepoint
 * ([com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils.applyCounterPlacementModifiers])
 * and expires per [GrantCounterPlacementModifierEffect.duration] via
 * `CleanupPhaseManager.cleanupEndOfTurn`.
 *
 * Used by Prairie Dog (OTJ): "{4}{W}: Until end of turn, if you would put one or more +1/+1
 * counters on a creature you control, put that many plus one +1/+1 counters on it instead."
 */
class GrantCounterPlacementModifierExecutor : EffectExecutor<GrantCounterPlacementModifierEffect> {

    override val effectType: KClass<GrantCounterPlacementModifierEffect> =
        GrantCounterPlacementModifierEffect::class

    override fun execute(
        state: GameState,
        effect: GrantCounterPlacementModifierEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId

        val entry = ActiveCounterPlacementModifier(
            modifier = effect.modifier,
            controllerId = controllerId,
            counterType = effect.counterType,
            recipient = effect.recipient,
            duration = effect.duration
        )

        val newState = state.copy(
            activeCounterPlacementModifiers = state.activeCounterPlacementModifiers + entry
        )

        return EffectResult.success(newState, emptyList())
    }
}
