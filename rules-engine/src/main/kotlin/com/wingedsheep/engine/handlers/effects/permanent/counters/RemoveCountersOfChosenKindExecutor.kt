package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.AddCountersOfChosenKindContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersOfChosenKindEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for [RemoveCountersOfChosenKindEffect] — the removing sibling of
 * [AddCountersOfChosenKindExecutor], with the same kind contract.
 *
 * [CounterKindSource.ITERATION] skips the choice: the kind is the one the enclosing counter-kind
 * loop bound as `chosenCounterType` (none → no-op) — Dramatist's Puppet's "or remove one from it".
 * [CounterKindSource.RECIPIENT] reads the kinds on the target at resolution: none → no-op; one →
 * removed with no prompt; two or more → the controller picks through a [ChooseOptionDecision],
 * resumed by [AddCountersOfChosenKindContinuation] with `remove = true`. Removal always goes
 * through [RemoveCountersEffect], so it takes off only what is there and emits the usual event.
 */
class RemoveCountersOfChosenKindExecutor(
    amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<RemoveCountersOfChosenKindEffect> {

    override val effectType: KClass<RemoveCountersOfChosenKindEffect> = RemoveCountersOfChosenKindEffect::class

    private val removeCounters = RemoveCountersExecutor(amountEvaluator)

    override fun execute(
        state: GameState,
        effect: RemoveCountersOfChosenKindEffect,
        context: EffectContext
    ): EffectResult {
        if (effect.count <= 0) return EffectResult.success(state, emptyList())
        val targetId = if (effect.target is EffectTarget.PlayerRef) {
            context.resolvePlayerTarget(effect.target, state)
        } else {
            context.resolveTarget(effect.target, state)
        } ?: return EffectResult.success(state, emptyList())

        fun remove(kind: CounterType) = removeCounters.execute(
            state,
            RemoveCountersEffect(kind, DynamicAmount.Fixed(effect.count), EffectTarget.SpecificEntity(targetId)),
            context
        )

        if (effect.kindFrom == CounterKindSource.ITERATION) {
            val kind = context.chosenCounterType ?: return EffectResult.success(state, emptyList())
            return remove(kind)
        }

        val kinds = state.getEntity(targetId)?.get<CountersComponent>()?.counters
            ?.filterValues { it > 0 }?.keys?.toList().orEmpty()
        if (kinds.isEmpty()) return EffectResult.success(state, emptyList())
        if (kinds.size == 1) return remove(kinds.single())

        val targetName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decision = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose a kind of counter to remove from $targetName",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = kinds.map { it.printed }
        ) }

        val continuation = AddCountersOfChosenKindContinuation(
            recipientId = targetId,
            controllerId = context.controllerId,
            counterKinds = kinds,
            count = effect.count,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            remove = true
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "CHOOSE_OPTION"))
    }
}
