package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.KeywordActionReplacements
import com.wingedsheep.engine.handlers.effects.ReplaceableKeywordAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConniveEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EmitConnivedEventEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for [ConniveEffect] — "[subject] connives" (CR 701.50).
 *
 * The connive proper is [ConniveEffect.body], the ordinary draw → discard → conditional +1/+1
 * counter pipeline the SDK composes; this executor adds only the two things the wrapper exists for.
 *
 * **Replacement (CR 614).** Applicable
 * [com.wingedsheep.sdk.scripting.ModifyKeywordAction] replacements on the battlefield are consulted
 * through [KeywordActionReplacements], exactly as [ExploreEffectExecutor] does for explore — connive
 * isn't dispatched as a generic replaceable event, so it's checked here directly. On a match the
 * connive is re-issued as `Composite(prefixEffects…, ConniveEffect(sameSubject,
 * replacementsApplied = true))` through the registry [recurse] runner, so:
 *  - the prefix runs *before* the connive, as printed ("instead you draw a card, then that creature
 *    connives") — which matters, because the extra card is in hand before the discard is chosen;
 *  - the composite executor's pause-sequencing carries the connive's own discard decision;
 *  - `replacementsApplied` stops the replacement applying to its own re-issue (CR 614.5), while two
 *    separate sources each still contribute their prefix.
 *
 * **Connive N (CR 701.50d/e).** [ConniveEffect.count] is evaluated only for the zero check: a
 * connive 0 is no connive at all. The body computes and stores N itself.
 *
 * **Observation (CR 701.50f).** [EmitConnivedEventEffect] is appended as the pipeline's last step
 * rather than emitted inline here: connive pauses mid-way for the discard choice, and an event
 * emitted in a paused batch does not reliably fire watcher triggers. Because it is a pipeline step
 * it also fires when the draw or the discard was impossible, which is what CR 701.50f requires.
 *
 * The subject is resolved to a concrete entity up front and re-bound as
 * [EffectTarget.SpecificEntity], so the re-issue and the tail event both refer to the permanent that
 * was conniving when the action began, not to whatever `Self`/`TriggeringEntity` would resolve to
 * after the pause.
 *
 * @param recurse registry entry point for delegating the Composite (handed to [PermanentExecutors]
 *   by the registry at construction).
 */
class ConniveEffectExecutor(
    private val recurse: (GameState, Effect, EffectContext) -> EffectResult,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<ConniveEffect> {

    override val effectType: KClass<ConniveEffect> = ConniveEffect::class

    override fun execute(
        state: GameState,
        effect: ConniveEffect,
        context: EffectContext
    ): EffectResult {
        // CR 701.50e: a permanent that would connive 0 doesn't connive — nothing is drawn or
        // discarded, no connive replacement applies and no connive event fires. Plain connive is a
        // fixed 1, so only connive N ever reaches the evaluator.
        if (effect.count != DynamicAmount.Fixed(1) &&
            predicateEvaluator.amounts.evaluate(state, effect.count, context) <= 0
        ) {
            return EffectResult.success(state)
        }

        // An unresolvable subject (the permanent left the battlefield before the connive ran) still
        // draws and discards — that is what the bare pipeline did before it was wrapped, and CR
        // 701.50b keeps the connive happening off last known information. What is skipped is the
        // part that genuinely needs a live permanent to match against: the replacement lookup and
        // the connive event. LKI-based matching for those is unmodeled; no printed card needs it.
        val connivingId = context.resolveTarget(effect.subject, state)
            ?: return recurse(state, effect.body, context)
        val subject = EffectTarget.SpecificEntity(connivingId)

        if (!effect.replacementsApplied) {
            val prefixEffects = KeywordActionReplacements.collectPrefixes(
                state, connivingId, ReplaceableKeywordAction.CONNIVE,
                predicateEvaluator = predicateEvaluator
            )
            if (prefixEffects.isNotEmpty()) {
                val composite = CompositeEffect(
                    prefixEffects + effect.copy(subject = subject, replacementsApplied = true)
                )
                return recurse(state, composite, context)
            }
        }

        return recurse(
            state,
            CompositeEffect(listOf(effect.body, EmitConnivedEventEffect(subject))),
            context
        )
    }
}
