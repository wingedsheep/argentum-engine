package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TurnHijackedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.HijackNextTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for HijackNextTurnEffect.
 *
 * PR 1 ships this as a no-op that emits a [TurnHijackedEvent] for logging and downstream
 * wiring. The full Mindslaver-style mechanic — input routing, hand visibility, end-of-turn
 * cleanup — lands in a follow-up PR alongside the new player-level component and
 * decision-routing seam in TurnManager / SubmitDecisionHandler / GameSession.getLegalActions.
 */
class HijackNextTurnExecutor : EffectExecutor<HijackNextTurnEffect> {

    override val effectType: KClass<HijackNextTurnEffect> = HijackNextTurnEffect::class

    override fun execute(
        state: GameState,
        effect: HijackNextTurnEffect,
        context: EffectContext
    ): EffectResult {
        val hijackedPlayerId = context.resolvePlayerTarget(effect.target)
            ?: return EffectResult.error(state, "No target player for HijackNextTurnEffect")

        val sourceId = context.sourceId ?: hijackedPlayerId
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Hijack"

        return EffectResult.success(
            state,
            listOf(
                TurnHijackedEvent(
                    controllerId = context.controllerId,
                    hijackedPlayerId = hijackedPlayerId,
                    sourceId = sourceId,
                    sourceName = sourceName
                )
            )
        )
    }
}
