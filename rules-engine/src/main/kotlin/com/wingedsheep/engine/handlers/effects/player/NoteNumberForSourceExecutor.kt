package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.sdk.scripting.effects.NoteNumberForSourceEffect
import kotlin.reflect.KClass

/**
 * Executor for [NoteNumberForSourceEffect].
 *
 * Evaluates the amount once and writes it as a [ChoiceValue.NumberChoice] into the source
 * permanent's cast-choices bag under the effect's slot, replacing any prior note — the
 * non-interactive sibling of [ChooseNumberForSourceExecutor]'s resumer. With no source, or a
 * source that is no longer the object the ability came from, nothing is noted.
 */
class NoteNumberForSourceExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<NoteNumberForSourceEffect> {

    override val effectType: KClass<NoteNumberForSourceEffect> = NoteNumberForSourceEffect::class

    override fun execute(
        state: GameState,
        effect: NoteNumberForSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        if (state.getEntity(sourceId) == null ||
            !context.objectReferences.isCurrent(context.objectReferences.source, state)) {
            return EffectResult.success(state)
        }
        val value = amountEvaluator.evaluate(state, effect.amount, context)
        return EffectResult.success(
            state.updateEntity(sourceId) { it.withCastChoice(effect.slot, ChoiceValue.NumberChoice(value)) }
        )
    }
}
