package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipDrawStepComponent
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.scripting.effects.SkipNextStepOrPhaseEffect
import kotlin.reflect.KClass

/**
 * Executor for [SkipNextStepOrPhaseEffect]: arms the one-shot marker for the target player's next
 * [SkipNextStepOrPhaseEffect.part].
 *
 * - [TurnPart.DRAW_STEP] adds [SkipDrawStepComponent], consumed by
 *   [com.wingedsheep.engine.core.DrawPhaseManager.performDrawStep].
 * - [TurnPart.UNTAP_STEP] adds one pending skip to [SkipNextUntapStepComponent]; a second skip on
 *   the same player stacks rather than overwriting (CR 614.10a — they skip the next two untap
 *   steps).
 */
class SkipNextStepOrPhaseExecutor : EffectExecutor<SkipNextStepOrPhaseEffect> {

    override val effectType: KClass<SkipNextStepOrPhaseEffect> = SkipNextStepOrPhaseEffect::class

    override fun execute(
        state: GameState,
        effect: SkipNextStepOrPhaseEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerTarget(effect.target, context, state)
            ?: return EffectResult.error(state, "No valid player for SkipNextStepOrPhaseEffect")

        val newState = state.updateEntity(playerId) { container ->
            when (effect.part) {
                TurnPart.DRAW_STEP -> container.with(SkipDrawStepComponent)
                TurnPart.UNTAP_STEP -> {
                    val pending = container.get<SkipNextUntapStepComponent>()?.steps ?: 0
                    container.with(SkipNextUntapStepComponent(steps = pending + 1))
                }
                // Rejected at construction (SkipNextStepOrPhaseEffect.SUPPORTED).
                TurnPart.MAIN_PHASE, TurnPart.COMBAT_PHASE -> container
            }
        }
        return EffectResult.success(newState)
    }
}
