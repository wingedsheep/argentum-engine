package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.sdk.scripting.effects.SkipNextTurnEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [SkipNextTurnEffect].
 *
 * Evaluates the effect's [count][SkipNextTurnEffect.count] and accumulates a
 * [SkipNextTurnComponent] on the target player so that many of their upcoming turns are skipped.
 * A resolved count of 0 (e.g. zero coins came up heads on Ral Zarek's ultimate) is a no-op.
 */
class SkipNextTurnExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<SkipNextTurnEffect> {

    override val effectType: KClass<SkipNextTurnEffect> = SkipNextTurnEffect::class

    override fun execute(
        state: GameState,
        effect: SkipNextTurnEffect,
        context: EffectContext
    ): EffectResult {
        val targetPlayerId = when (val target = effect.target) {
            is EffectTarget.PlayerRef ->
                TargetResolutionUtils.resolvePlayerRef(target.player, context, state)
            else -> TargetResolutionUtils.resolveTarget(target, context, state)
        } ?: return EffectResult.error(state, "Cannot resolve player for SkipNextTurnEffect")

        val turns = amountEvaluator.evaluate(state, effect.count, context)
        if (turns <= 0) return EffectResult.success(state)

        val newState = state.updateEntity(targetPlayerId) { container ->
            val existing = container.get<SkipNextTurnComponent>() ?: SkipNextTurnComponent(0)
            container.with(existing.copy(turns = existing.turns + turns))
        }

        return EffectResult.success(newState)
    }
}
