package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RecordValueAsEnteredEffect
import kotlin.reflect.KClass

/**
 * Executor for [RecordValueAsEnteredEffect] — evaluates the amount once and stamps it on the
 * entering permanent (the effect's source) as its
 * [com.wingedsheep.engine.state.components.battlefield.EnteredWithValueComponent], the same stamp
 * [PayAnyAmountOfLifeAsEntersExecutor] writes. Wood Elemental's characteristic-defining P/T reads
 * it back during projection; it goes with the object on a zone change (CR 400.7).
 */
class RecordValueAsEnteredExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<RecordValueAsEnteredEffect> {

    override val effectType: KClass<RecordValueAsEnteredEffect> = RecordValueAsEnteredEffect::class

    override fun execute(
        state: GameState,
        effect: RecordValueAsEnteredEffect,
        context: EffectContext
    ): EffectResult {
        val permanentId = context.sourceId
            ?: return EffectResult.error(state, "No entering permanent to record a value on")
        val value = maxOf(amountEvaluator.evaluate(state, effect.amount, context), 0)
        return EffectResult.success(PayAnyAmountOfLifeAsEntersExecutor.recordValue(state, permanentId, value))
    }
}
