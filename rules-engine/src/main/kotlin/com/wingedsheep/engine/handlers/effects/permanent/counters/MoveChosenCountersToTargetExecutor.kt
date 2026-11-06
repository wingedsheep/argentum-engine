package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [MoveChosenCountersToTargetEffect].
 *
 * "Move one or more counters from Goldberry onto another target permanent you control.
 * If you do, draw a card." (Goldberry, River-Daughter — ability B.) "Move a counter from target
 * permanent you control onto a second target permanent." (Nesting Grounds.)
 *
 * Starts the prompt-per-(source, kind) walk in [MoveChosenCountersFlow], bounded by the effect's
 * `maxTotal` / `minTotal`, over every kind on the source — or only the effect's `counterType`. A
 * [EffectTarget.GroupRef] source walks every member of the group in one move (Aetherborn
 * Marauder's "from other permanents you control"). When the walk finishes without a prompt (every
 * step forced) and [MoveChosenCountersToTargetEffect.drawCardOnMove] is set, the draw runs here through [recursion];
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
        val destinationId = context.resolveTarget(effect.destination, state)
            ?: return EffectResult.success(state, emptyList())
        // Counters move between permanents: a destination that has left the battlefield (an enters
        // trigger whose creature died in response) takes nothing.
        val battlefield = state.getBattlefield()
        if (destinationId !in battlefield) return EffectResult.success(state, emptyList())
        val maxTotal = effect.maxTotal
        if (maxTotal != null && maxTotal <= 0) return EffectResult.success(state, emptyList())

        // One source, or every member of a group ("from other permanents you control").
        val sources = when (val source = effect.source) {
            is EffectTarget.GroupRef -> BattlefieldFilterUtils.resolveGroup(state, source.filter, context, predicates)
            else -> listOfNotNull(context.resolveTarget(source, state))
        }.filter { it != destinationId && it in battlefield }

        val steps = sources.flatMap { sourceId ->
            val counters = state.getEntity(sourceId)?.get<CountersComponent>()?.counters.orEmpty()
            counters.entries
                .filter { it.value > 0 && (effect.counterType == null || it.key == effect.counterType) }
                .map { sourceId to it.key }
        }
        if (steps.isEmpty()) return EffectResult.success(state, emptyList())

        val move = MoveChosenCountersFlow.Move(
            destinationId = destinationId,
            controllerId = context.controllerId,
            destinationName = state.getEntity(destinationId)?.get<CardComponent>()?.name ?: "",
            drawCardOnMove = effect.drawCardOnMove
        )
        // The floor can't exceed the budget or what's actually there — sources carrying no more
        // than the floor simply give up everything they have.
        val available = steps.sumOf { (id, kind) -> RemoveAnyNumberOfCountersFlow.countOf(state, id, kind) }
        val floor = effect.minTotal
            .coerceAtMost(maxTotal ?: Int.MAX_VALUE)
            .coerceAtMost(available)

        return when (
            val outcome = MoveChosenCountersFlow.advance(
                state, move, steps, maxTotal, floor, pending = emptyList(), anyMovedSoFar = false,
                predicates = predicates
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
