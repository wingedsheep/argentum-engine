package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * How many modes a *cast* of a modal spell may and must choose (CR 700.2a, CR 700.2d).
 *
 * The single authority for the question, shared by the two places that ask it: `CastSpellHandler`
 * validating a submitted cast, and `CastSpellEnumerator` advertising one. They used to compute it
 * separately and had already drifted — the enumerator knew nothing about the blight path and
 * nothing about the floor, so it offered mode counts the handler rejected and dropped variants the
 * handler would have taken.
 *
 * Three shapes, in precedence order:
 *
 * - [ModalEffect.chooseAllIfBlightPaid] — all modes when the spell's `BlightOrPay` cost went down
 *   the blight path, the printed floor otherwise (Pyrrhic Strike).
 * - [ModalEffect.dynamicChooseCount] / [ModalEffect.dynamicMinChooseCount] — evaluated against
 *   cast-time state. The ceiling alone is "you *may* choose two instead" (Flame of Anor); both
 *   together are "choose both **instead**" (the teamwork modals), which is mandatory.
 * - Neither — the printed `[minChooseCount, chooseCount]`.
 *
 * The evaluation context carries [declaredCostSlot], the declaration *this cast* is making, because
 * a count may branch on it: `Conditions.TeamworkWasPaid` has to answer while the card is still in
 * hand, and the durable `CastChoicesComponent` it otherwise reads only exists once the spell has
 * resolved (CR 601.2b).
 */
object ModalChooseCounts {

    /**
     * The inclusive `min..max` range of mode counts this cast may choose. Both ends are clamped to
     * `[minChooseCount, modes.size]` — or to `[minChooseCount, ∞)` when [ModalEffect.allowRepeat]
     * lets one mode fill every pick — and `min` never exceeds `max`.
     */
    fun forCast(
        state: GameState,
        modalEffect: ModalEffect,
        cardId: EntityId,
        controllerId: EntityId,
        declaredCostSlot: ChoiceSlot?,
        blightPaid: Boolean,
        conditionEvaluator: ConditionEvaluator
    ): IntRange {
        if (modalEffect.chooseAllIfBlightPaid) {
            return if (blightPaid) {
                modalEffect.modes.size..modalEffect.modes.size
            } else {
                modalEffect.minChooseCount..modalEffect.minChooseCount
            }
        }

        val dynamicMax = modalEffect.dynamicChooseCount
        if (dynamicMax == null) {
            return modalEffect.minChooseCount..modalEffect.chooseCount
        }

        val context = EffectContext(
            sourceId = cardId,
            controllerId = controllerId,
            targets = emptyList(),
            xValue = 0,
            declaredCostSlot = declaredCostSlot
        )
        val evaluator = DynamicAmountEvaluator(conditionEvaluator = conditionEvaluator)
        // The mode list caps the count only when each mode can be picked once. With
        // [ModalEffect.allowRepeat] the same mode stays on the menu for every pick (CR 700.2d), so
        // a three-mode spell can absorb any number of picks and clamping to `modes.size` would
        // silently shrink the evaluated count. ModalEffectExecutor makes the same distinction for
        // the resolution-time path; keep the two in step.
        val ceiling = if (modalEffect.allowRepeat) Int.MAX_VALUE else modalEffect.modes.size
        fun evaluate(amount: com.wingedsheep.sdk.scripting.values.DynamicAmount) =
            evaluator.evaluate(state, amount, context)
                .coerceIn(modalEffect.minChooseCount, maxOf(modalEffect.minChooseCount, ceiling))

        val max = evaluate(dynamicMax)
        val min = modalEffect.dynamicMinChooseCount?.let { evaluate(it) } ?: modalEffect.minChooseCount
        return minOf(min, max)..max
    }
}
