package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.AddCountersOfChosenKindContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersOfChosenKindEffect
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [AddCountersOfChosenKindEffect] — "choose a kind of counter on target permanent.
 * Put an additional counter of that kind on that permanent" (Ichormoon Gauntlet).
 *
 * [CounterKindSource.ITERATION] skips the choice: the kind is the one the enclosing
 * `IterationSpace.CounterKindsAmong` loop bound as `chosenCounterType` (none → no-op).
 *
 * Reads the candidate kinds at resolution — those on the recipient, or for
 * [CounterKindSource.LISTED_ABSENT] the printed kinds the recipient has none of. None → no-op; one →
 * placed with no prompt (the only legal choice); `atRandom` → drawn uniformly through the game RNG;
 * otherwise two or more → the controller picks through a [ChooseOptionDecision], resumed
 * by [AddCountersOfChosenKindContinuation]. Placement always goes through [AddCountersEffect], so
 * counter-placement replacements and counter-placed triggers see it like any other placement.
 */
class AddCountersOfChosenKindExecutor(
    predicateEvaluator: PredicateEvaluator
) : EffectExecutor<AddCountersOfChosenKindEffect> {

    override val effectType: KClass<AddCountersOfChosenKindEffect> = AddCountersOfChosenKindEffect::class

    private val addCounters = AddCountersExecutor(predicateEvaluator)

    override fun execute(
        state: GameState,
        effect: AddCountersOfChosenKindEffect,
        context: EffectContext
    ): EffectResult {
        if (effect.count <= 0) return EffectResult.success(state, emptyList())
        val recipientId = if (effect.target is EffectTarget.PlayerRef) {
            context.resolvePlayerTarget(effect.target, state)
        } else {
            context.resolveTarget(effect.target, state)
        } ?: return EffectResult.success(state, emptyList())

        if (!state.projectedState.canReceiveCounters(recipientId)) {
            return EffectResult.success(state, emptyList())
        }

        if (effect.kindFrom == CounterKindSource.ITERATION) {
            val kind = context.chosenCounterType ?: return EffectResult.success(state, emptyList())
            return addCounters.execute(
                state,
                AddCountersEffect(kind, effect.count, EffectTarget.SpecificEntity(recipientId)),
                context
            )
        }

        val present = state.getEntity(recipientId)?.get<CountersComponent>()?.counters
            ?.filterValues { it > 0 }?.keys?.toList().orEmpty()
        val kinds = if (effect.kindFrom == CounterKindSource.LISTED_ABSENT) {
            effect.kinds.distinct().filterNot { it in present }
        } else {
            present
        }.filter { state.projectedState.canReceiveCounters(recipientId, it) }
        if (kinds.isEmpty()) return EffectResult.success(state, emptyList())

        if (kinds.size == 1 || effect.atRandom) {
            val (kind, drawnState) =
                if (kinds.size == 1) kinds.single() to state else state.nextRandom { pick(kinds) }
            return addCounters.execute(
                drawnState,
                AddCountersEffect(kind, effect.count, EffectTarget.SpecificEntity(recipientId)),
                context
            )
        }

        val recipientName = state.getEntity(recipientId)?.get<CardComponent>()?.name ?: ""
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val prompt = if (effect.kindFrom == CounterKindSource.LISTED_ABSENT) {
            "Choose a kind of counter to put " +
                (if (effect.count == 1) "on" else "${effect.count} of on") + " $recipientName"
        } else {
            "Choose a kind of counter on $recipientName to add " +
                (if (effect.count == 1) "another" else "${effect.count} more") + " of"
        }
        val decision = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = kinds.map { it.printed }
        ) }

        val continuation = AddCountersOfChosenKindContinuation(
            recipientId = recipientId,
            controllerId = context.controllerId,
            counterKinds = kinds,
            count = effect.count,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "CHOOSE_OPTION"))
    }
}
