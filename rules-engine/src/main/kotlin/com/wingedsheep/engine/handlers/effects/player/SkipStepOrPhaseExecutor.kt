package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipDrawStepComponent
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.state.components.player.SkippedTurnPartsComponent
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.scripting.effects.SkipDuration
import com.wingedsheep.sdk.scripting.effects.SkipStepOrPhaseEffect
import kotlin.reflect.KClass

/**
 * Executor for [SkipStepOrPhaseEffect].
 *
 * [SkipDuration.NEXT] arms a one-shot marker for the target player's next part:
 * - [TurnPart.DRAW_STEP] adds [SkipDrawStepComponent], consumed by
 *   [com.wingedsheep.engine.core.DrawPhaseManager.performDrawStep].
 * - [TurnPart.UNTAP_STEP] adds one pending skip to [SkipNextUntapStepComponent]; a second skip on
 *   the same player stacks rather than overwriting (CR 614.10a — they skip the next two untap
 *   steps).
 *
 * [SkipDuration.THIS_TURN] adds the part to the player's [SkippedTurnPartsComponent], which
 * `TurnManager.advanceStepFromEndedStep` consults before it enters each step. The set is additive,
 * so two sources naming different parts in the same turn both stick, and end-of-turn cleanup drops
 * the whole component.
 */
class SkipStepOrPhaseExecutor : EffectExecutor<SkipStepOrPhaseEffect> {

    override val effectType: KClass<SkipStepOrPhaseEffect> = SkipStepOrPhaseEffect::class

    override fun execute(
        state: GameState,
        effect: SkipStepOrPhaseEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerTarget(effect.target, context, state)
            ?: return EffectResult.error(state, "No valid player for SkipStepOrPhaseEffect")

        val newState = state.updateEntity(playerId) { container ->
            when (effect.duration) {
                SkipDuration.THIS_TURN -> {
                    val existing = container.get<SkippedTurnPartsComponent>()?.parts ?: emptySet()
                    container.with(SkippedTurnPartsComponent(existing + effect.part))
                }
                SkipDuration.NEXT -> when (effect.part) {
                    TurnPart.DRAW_STEP -> container.with(SkipDrawStepComponent)
                    TurnPart.UNTAP_STEP -> {
                        val pending = container.get<SkipNextUntapStepComponent>()?.steps ?: 0
                        container.with(SkipNextUntapStepComponent(steps = pending + 1))
                    }
                    TurnPart.MAIN_PHASE, TurnPart.COMBAT_PHASE ->
                        error("A next-instance skip of ${effect.part} is rejected at construction")
                }
            }
        }
        return EffectResult.success(newState)
    }
}
