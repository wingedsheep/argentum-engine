package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyLifeGain

/**
 * Shared application path for [ModifyLifeGain] replacement effects (Alhammarret's Archive,
 * Leyline of Hope, etc.). Called from every life-gain emission point —
 * [com.wingedsheep.engine.handlers.effects.life.GainLifeExecutor],
 * [com.wingedsheep.engine.handlers.effects.life.OwnerGainsLifeExecutor], and the lifelink
 * damage → life path in [DamageUtils] — so any source of life gain runs through the same
 * replacement filter.
 *
 * Each modifier maps `amount -> (amount * multiplier) + modifier`, clamped to ≥ 0. Per the Leyline of
 * Hope rulings, the modifier applies **once per life-gain event** regardless of how the
 * original amount was assembled — so this helper is invoked once per receiving player per
 * event, not per "+1" pip.
 */
object LifeGainModifiers {

    /**
     * Every [ModifyLifeGain] that would apply to [recipientId] gaining [amount] life, gathered
     * through the shared [ReplacementEffectProcessor] so this fast path sees exactly what the
     * full CR 614 pipeline sees: sources that have lost all abilities or are face down are
     * skipped, text changes apply, the recipient filter and `restrictions` are read the same way,
     * and granted or floating modifiers count too.
     */
    fun gather(state: GameState, recipientId: EntityId, amount: Int, predicateEvaluator: PredicateEvaluator): List<ModifyLifeGain> {
        if (amount <= 0) return emptyList()
        return ReplacementEffectProcessor(predicateEvaluator.conditions)
            .gatherReplacements(state, PendingGameEvent.LifeGainPending(recipientId, amount))
            .mapNotNull { it.effect as? ModifyLifeGain }
    }

    /**
     * Whether the order in which [modifiers] apply changes the result — the case where CR 616.1
     * has the affected player choose. Each modifier is the affine map `x -> a*x + b`; two of them
     * commute exactly when `b1*(a2-1) == b2*(a1-1)`, so same-shape stacks (several Leylines of
     * Hope, several doublers) never need a choice while a doubler next to a "+1" does.
     */
    fun orderMatters(modifiers: List<ModifyLifeGain>): Boolean =
        modifiers.indices.any { i ->
            (i + 1 until modifiers.size).any { j ->
                val f = modifiers[i]
                val g = modifiers[j]
                f.modifier * (g.multiplier - 1) != g.modifier * (f.multiplier - 1)
            }
        }

    /**
     * Apply every applicable [ModifyLifeGain] to a single life-gain event. Returns the modified
     * amount (≥ 0). Callers route a gain whose modifiers don't commute ([orderMatters]) through
     * the replacement processor instead, so the affected player picks the order; here every
     * order gives the same answer.
     *
     * @param state Current game state
     * @param recipientId The player who is gaining life
     * @param originalAmount The life amount the event would normally apply
     */
    fun apply(state: GameState, recipientId: EntityId, originalAmount: Int, predicateEvaluator: PredicateEvaluator): Int {
        if (originalAmount <= 0) return originalAmount
        return gather(state, recipientId, originalAmount, predicateEvaluator)
            .fold(originalAmount) { amount, effect -> (amount * effect.multiplier + effect.modifier).coerceAtLeast(0) }
    }
}
