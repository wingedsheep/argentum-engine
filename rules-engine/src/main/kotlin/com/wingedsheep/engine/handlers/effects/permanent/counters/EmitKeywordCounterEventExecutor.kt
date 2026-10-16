package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EvolvedEvent
import com.wingedsheep.engine.core.TrainedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.EmitKeywordCounterEventEffect
import kotlin.reflect.KClass

/**
 * Tail of the `training()` and `evolve()` triggered abilities: emits a [TrainedEvent] (CR 702.149c)
 * or an [EvolvedEvent] (CR 702.100b) after the ability's preceding
 * [com.wingedsheep.sdk.scripting.effects.AddCountersEffect] resolves, so "when this creature
 * trains / evolves" payoffs (Savior of Ollenbock, Watchful Radstag) fire.
 *
 * Both rules read "a resolving [keyword] ability puts one or more +1/+1 counters on this creature",
 * so the event must fire **only when the counter actually landed**. This recomputes the placement
 * decision the ability's `AddCountersEffect` just made — on the post-placement state, which still
 * carries the same battlefield replacement / duration modifiers and the same "can't have counters put
 * on it" prohibition:
 *
 *  - the creature ([EffectContext.sourceId]) must still be on the battlefield — if it left in
 *    response to the trigger, its `AddCounters` found no target and placed nothing;
 *  - it must be able to receive counters (a Solemnity-type prohibition places zero, and then no
 *    event fires);
 *  - the placed count, one +1/+1 counter after counter-placement replacements (Hardened Scales,
 *    Doubling Season) and duration modifiers, must be `>= 1`.
 *
 * The recomputation is deterministic: the modifier sources persist across the sibling placement, so it
 * yields exactly the count `AddCountersExecutor` placed. Card authors should not use
 * [EmitKeywordCounterEventEffect] directly; it is wired into `training()` and `evolve()`.
 */
class EmitKeywordCounterEventExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<EmitKeywordCounterEventEffect> {

    override val effectType: KClass<EmitKeywordCounterEventEffect> = EmitKeywordCounterEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitKeywordCounterEventEffect,
        context: EffectContext
    ): EffectResult {
        val creatureId = context.sourceId ?: return EffectResult.success(state)

        // The creature must still be on the battlefield — if it left in response to the trigger,
        // the sibling AddCounters placed nothing.
        if (creatureId !in state.getBattlefield()) return EffectResult.success(state)

        // "Puts one or more +1/+1 counters": mirror the placement AddCountersExecutor performed.
        // A "can't have counters put on it" prohibition means zero counters landed — no event.
        if (!state.projectedState.canReceiveCounters(creatureId)) return EffectResult.success(state)

        val placed = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state,
            creatureId,
            CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            placerId = context.controllerId,
            predicateEvaluator = predicateEvaluator, byEffect = true
        )
        if (placed < 1) return EffectResult.success(state)

        val sourceName = state.getEntity(creatureId)?.get<CardComponent>()?.name ?: "Unknown"
        val event = when (effect.keyword) {
            Keyword.EVOLVE -> EvolvedEvent(
                evolvedId = creatureId,
                controllerId = context.controllerId,
                counters = placed,
                sourceName = sourceName
            )
            Keyword.TRAINING -> TrainedEvent(
                trainedId = creatureId,
                controllerId = context.controllerId,
                counters = placed,
                sourceName = sourceName
            )
            else -> error("EmitKeywordCounterEventEffect does not support ${effect.keyword}")
        }
        return EffectResult.success(state, listOf(event))
    }
}
