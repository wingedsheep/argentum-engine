package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkippedTurnPartsComponent
import com.wingedsheep.sdk.scripting.effects.SkipStepOrPhaseThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [SkipStepOrPhaseThisTurnEffect].
 *
 * Adds the chosen [com.wingedsheep.sdk.core.TurnPart] to the target player's
 * [SkippedTurnPartsComponent], which `TurnManager.advanceStepFromEndedStep` consults before it
 * enters each step. The set is additive, so two sources naming different parts in the same turn
 * both stick, and end-of-turn cleanup drops the whole component.
 */
class SkipStepOrPhaseThisTurnExecutor : EffectExecutor<SkipStepOrPhaseThisTurnEffect> {

    override val effectType: KClass<SkipStepOrPhaseThisTurnEffect> = SkipStepOrPhaseThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: SkipStepOrPhaseThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val targetPlayerId = context.resolvePlayerTarget(effect.target, state)
            ?: return EffectResult.error(state, "Cannot resolve player for SkipStepOrPhaseThisTurnEffect")

        val newState = state.updateEntity(targetPlayerId) { container ->
            val existing = container.get<SkippedTurnPartsComponent>()?.parts ?: emptySet()
            container.with(SkippedTurnPartsComponent(existing + effect.part))
        }

        return EffectResult.success(newState)
    }
}
