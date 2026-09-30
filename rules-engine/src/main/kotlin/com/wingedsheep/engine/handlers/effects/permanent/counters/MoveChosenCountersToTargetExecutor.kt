package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for [MoveChosenCountersToTargetEffect].
 *
 * "Move one or more counters from Goldberry onto another target permanent you control.
 * If you do, draw a card." (Goldberry, River-Daughter — ability B.) "Move a counter from target
 * permanent you control onto a second target permanent." (Nesting Grounds.)
 *
 * Starts the prompt-per-kind walk in [MoveChosenCountersFlow], bounded by the effect's
 * `maxTotal` / `minTotal`. When the walk finishes without a prompt (every step forced) and
 * [MoveChosenCountersToTargetEffect.drawCardOnMove] is set, the draw runs here through [recursion];
 * otherwise the continuation resumer draws after the last answer.
 */
class MoveChosenCountersToTargetExecutor(
    private val predicates: PredicateEvaluator,
    private val recursion: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<MoveChosenCountersToTargetEffect> {

    override val effectType: KClass<MoveChosenCountersToTargetEffect> =
        MoveChosenCountersToTargetEffect::class

    override fun execute(
        state: GameState,
        effect: MoveChosenCountersToTargetEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.resolveTarget(effect.source, state)
            ?: return EffectResult.success(state, emptyList())
        val destinationId = context.resolveTarget(effect.destination, state)
            ?: return EffectResult.success(state, emptyList())
        if (sourceId == destinationId) return EffectResult.success(state, emptyList())
        val maxTotal = effect.maxTotal
        if (maxTotal != null && maxTotal <= 0) return EffectResult.success(state, emptyList())

        val counters = state.getEntity(sourceId)?.get<CountersComponent>()
            ?: return EffectResult.success(state, emptyList())
        val present = counters.counters.entries.filter { it.value > 0 }.map { it.key }
        if (present.isEmpty()) return EffectResult.success(state, emptyList())

        val move = MoveChosenCountersFlow.Move(
            sourceId = sourceId,
            destinationId = destinationId,
            controllerId = context.controllerId,
            sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "",
            destinationName = state.getEntity(destinationId)?.get<CardComponent>()?.name ?: "",
            drawCardOnMove = effect.drawCardOnMove
        )
        // The floor can't exceed the budget or what's actually there — a source carrying no more
        // than the floor simply gives up everything it has.
        val floor = effect.minTotal
            .coerceAtMost(maxTotal ?: Int.MAX_VALUE)
            .coerceAtMost(counters.counters.values.sum())

        return when (
            val outcome = MoveChosenCountersFlow.advance(
                state, move, present, maxTotal, floor, anyMovedSoFar = false, predicates = predicates
            )
        ) {
            is MoveChosenCountersFlow.Outcome.Prompt -> EffectResult.propagatePause(outcome.state, outcome.events)
            is MoveChosenCountersFlow.Outcome.Done -> {
                if (!(effect.drawCardOnMove && outcome.anyMoved)) {
                    EffectResult.success(outcome.state, outcome.events)
                } else {
                    val draw = recursion(outcome.state, DrawCardsEffect(1), context)
                    EffectResult(draw.state, outcome.events + draw.events, draw.outcome)
                }
            }
        }
    }
}
