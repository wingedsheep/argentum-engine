package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The one place a [DynamicAmount] carried by a **cost atom** is turned into a number.
 *
 * A cost is priced *before* anything resolves, so it can't go through
 * [com.wingedsheep.engine.handlers.DynamicAmountEvaluator] — there is no `EffectContext` yet, only
 * the half-announced spell or ability the cost belongs to. What that half-announcement does carry
 * is exactly the two things a printed cost is ever priced from: the X the caster chose, and the
 * targets they announced (CR 601.2b–c, both settled before the total cost is determined at
 * 601.2f). Everything else evaluates to 0.
 *
 * Before this existed the same `Fixed` / `XValue` `when` was hand-written in six places, which is
 * why the seventh shape — Urgent Necropsy's "collect evidence X, where X is the total mana value of
 * the permanents this spell targets" — would otherwise have had to be added six times.
 *
 * **Zero means zero, not "unknown".** For a counted cost a zero requirement is trivially met; for a
 * summed one (collect evidence) exiling nothing legitimately satisfies a threshold of 0. Callers
 * that need to distinguish "cannot be priced yet" from "prices to 0" ask [dependsOnTargets] rather
 * than reading a sentinel out of the number.
 */
object CostAtomAmounts {

    /**
     * Evaluate [amount] for a cost being paid by the controller of the spell/ability it belongs to.
     *
     * @param xValue the X declared for this cast/activation (`CastSpell.xValue`,
     *   `CostPaymentChoices.xValue`), or null where the context has none.
     * @param targets the targets already announced for the object this cost is being paid for.
     *   Empty in every context that has no target list of its own — a resolution-time `PayCost`, a
     *   ward payment, an affordability probe run before targets are chosen. A target-derived amount
     *   then reads 0, which is why [dependsOnTargets] exists for the callers that must not treat
     *   that as the final price.
     * @param sourceId the spell the cost is paid for (`CastSpell.cardId`). Prices the "equal to its
     *   mana value" amount ([CostAtom.PayPlayerCounters.SOURCE_MANA_VALUE]); without one — every
     *   activated-ability site — that amount can't be priced and is **unpayable** ([UNPRICEABLE])
     *   rather than free.
     */
    fun evaluate(
        state: GameState,
        amount: DynamicAmount,
        xValue: Int? = null,
        targets: List<ChosenTarget> = emptyList(),
        sourceId: EntityId? = null,
    ): Int = when (amount) {
        is DynamicAmount.Fixed -> amount.amount
        is DynamicAmount.XValue -> xValue ?: 0
        // "An amount equal to its mana value" (Amped Raptor). A card's mana value is intrinsic
        // (CR 202.3) — read off the base CardComponent like every other mana-value read; X counts
        // as 0 off the stack, and a spell cast "rather than paying its mana cost" has X = 0 anyway
        // (CR 107.3b).
        CostAtom.PayPlayerCounters.SOURCE_MANA_VALUE ->
            sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.manaValue } ?: UNPRICEABLE
        is DynamicAmount.ContextProperty -> when (amount.key) {
            ContextPropertyKey.TARGETS_TOTAL_MANA_VALUE -> totalManaValueOf(state, targets)
            // Every other context key reads a trigger payload or a resolution pipeline, neither of
            // which exists while a cost is being priced.
            else -> 0
        }
        else -> 0
    }

    /**
     * Whether [amount] can only be priced once the object's targets are announced — the signal an
     * enumerator uses to publish a *deferred* cost rather than a wrong one, and an affordability
     * check uses to withhold judgement instead of failing closed on a price it hasn't got yet.
     */
    /**
     * The price of a source-dependent amount evaluated with no source to read. Large enough that no
     * player holds that many counters (so every affordability check fails closed), small enough that
     * summing a handful of them can't overflow.
     */
    const val UNPRICEABLE: Int = 1_000_000

    fun dependsOnTargets(amount: DynamicAmount): Boolean =
        amount is DynamicAmount.ContextProperty &&
            amount.key == ContextPropertyKey.TARGETS_TOTAL_MANA_VALUE

    /**
     * Summed mana value of the objects among [targets] (CR 202.3).
     *
     * Players have no mana value and contribute nothing, so a mixed "target creature and target
     * player" requirement measures only the objects. Read off the base [CardComponent] like every
     * other mana-value read in the engine: mana value is intrinsic, so a permanent's projection has
     * nothing different to say, and a token (no mana cost) is worth 0. An entity the engine can't
     * read is worth 0, which keeps the price from silently inflating.
     */
    fun totalManaValueOf(state: GameState, targets: List<ChosenTarget>): Int =
        targets.sumOf { manaValueOf(state, it) }

    private fun manaValueOf(state: GameState, target: ChosenTarget): Int {
        val entityId: EntityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Card -> target.cardId
            is ChosenTarget.Spell -> target.spellEntityId
            is ChosenTarget.Player -> return 0
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.manaValue ?: 0
    }
}
