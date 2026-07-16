package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TrainedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.EmitTrainedEventEffect
import kotlin.reflect.KClass

/**
 * Tail of the `training()` triggered ability (wired by [com.wingedsheep.sdk.dsl.training]): emits a
 * [TrainedEvent] (CR 702.149c) after the ability's preceding [com.wingedsheep.sdk.scripting.effects.AddCountersEffect]
 * resolves, so "When this creature trains" payoffs
 * ([com.wingedsheep.sdk.scripting.EventPattern.TrainedEvent], e.g. Savior of Ollenbock) fire.
 *
 * CR 702.149c: "'When this creature trains' means 'When a resolving training ability puts one or more
 * +1/+1 counters on this creature.'" The event must fire **only when the counter actually landed**,
 * so this recomputes the placement decision the training ability's `AddCountersEffect` just made — on
 * the post-placement state, which still carries the same battlefield replacement / duration modifiers
 * and the same "can't have counters put on it" prohibition:
 *
 *  - the trained creature ([EffectContext.sourceId]) must still be on the battlefield — if it left in
 *    response to the attack trigger, its `AddCounters` found no target and placed nothing;
 *  - it must be able to receive counters (a Solemnity-type prohibition is the one way a training
 *    ability places zero — CR 702.149c then does not fire);
 *  - the placed count, one +1/+1 counter after counter-placement replacements (Hardened Scales,
 *    Doubling Season) and duration modifiers, must be `>= 1`.
 *
 * The recomputation is deterministic: the modifier sources persist across the sibling placement, so it
 * yields exactly the count `AddCountersExecutor` placed. Card authors should not use
 * [EmitTrainedEventEffect] directly; it is wired into `training()`.
 */
class EmitTrainedEventExecutor : EffectExecutor<EmitTrainedEventEffect> {

    override val effectType: KClass<EmitTrainedEventEffect> = EmitTrainedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitTrainedEventEffect,
        context: EffectContext
    ): EffectResult {
        val trainedId = context.sourceId ?: return EffectResult.success(state)

        // The training creature must still be on the battlefield — if it left in response to the
        // attack trigger, the sibling AddCounters placed nothing, so it did not train.
        if (trainedId !in state.getBattlefield()) return EffectResult.success(state)

        // "Puts one or more +1/+1 counters": mirror the placement AddCountersExecutor performed.
        // A "can't have counters put on it" prohibition means zero counters landed — no train.
        if (!state.projectedState.canReceiveCounters(trainedId)) return EffectResult.success(state)

        val placed = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state,
            trainedId,
            CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            placerId = context.controllerId
        )
        if (placed < 1) return EffectResult.success(state)

        val sourceName = state.getEntity(trainedId)?.get<CardComponent>()?.name ?: "Unknown"

        return EffectResult.success(
            state,
            listOf(
                TrainedEvent(
                    trainedId = trainedId,
                    controllerId = context.controllerId,
                    counters = placed,
                    sourceName = sourceName
                )
            )
        )
    }
}
