package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MarkMustBlockThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [MarkMustBlockThisTurnEffect] — "target creature blocks this turn if able."
 *
 * Adds a `Layer.ABILITY` floating [SerializableModification.SetMustBlock] to the target for the
 * rest of the turn. That is the same projection the static `MustBlock` (Grand Melee) writes, so
 * `BlockPhaseManager.validateProjectedMustBlockRequirements` enforces it with no further wiring:
 * if the creature can legally block at least one attacker, declaring no block is rejected.
 *
 * A floating effect rather than a component (the shape [MarkMustAttackThisTurnExecutor] uses)
 * precisely because the "must block" reading already flows through projection — reusing it keeps
 * one enforcement path instead of two, and the `Duration.EndOfTurn` cleanup is the generic one.
 *
 * Silently no-ops on a target that has gone (the ability fizzled or the creature left the
 * battlefield): a requirement on a nonexistent permanent has nothing to constrain.
 */
class MarkMustBlockThisTurnExecutor : EffectExecutor<MarkMustBlockThisTurnEffect> {

    override val effectType: KClass<MarkMustBlockThisTurnEffect> = MarkMustBlockThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: MarkMustBlockThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val entityId = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?: return EffectResult.success(state)
        if (entityId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }
        state.getEntity(entityId)?.get<CardComponent>()
            ?: return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetMustBlock,
            affectedEntities = setOf(entityId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
