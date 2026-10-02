package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility

/*
 * Readings of what a [CastSpell] action declared (CR 601.2b) — which alternative cost, which optional
 * cost slot — shared by every stage of the casting pipeline.
 */

/**
 * True if this cast's [CastSpell.alternativeCostType] permits the given alternative cost [type] —
 * either because the player explicitly chose it, or because no choice was recorded (`null`, the
 * legacy path) and the handler should fall back to its priority chain. Used to gate each branch of
 * the alternative-cost resolution so an explicit choice (e.g. evoke) isn't overridden by a
 * higher-priority cost that also happens to be available (e.g. a granted warp).
 */
internal fun CastSpell.altAllows(type: AlternativeCostType): Boolean =
    alternativeCostType == null || alternativeCostType == type

/**
 * True if this cast is paying the card's cleave cost (CR 702.148). Cleave is an alternative cost,
 * so it's driven by [CastSpell.useAlternativeCost] gated on the chosen [AlternativeCostType.CLEAVE]
 * (never by `declaredCostSlot`, which names an *additional* cost). When true, the resolver swaps in the
 * brackets-removed effect / target-requirement variant (`cleaveSpellEffect` /
 * `cleaveTargetRequirements`).
 */
internal fun isCleaveCast(action: CastSpell, cardDef: com.wingedsheep.sdk.model.CardDefinition): Boolean =
    action.useAlternativeCost &&
        action.altAllows(AlternativeCostType.CLEAVE) &&
        cardDef.keywordAbilities.any { it is KeywordAbility.Cleave }

/**
 * True if this cast is paying the card's overload cost (CR 702.96). Like cleave, an alternative cost
 * chosen by [AlternativeCostType.OVERLOAD]. When true the spell has no targets (CR 702.96b) and
 * resolves with its "each" variant (`overloadSpellEffect`).
 */
internal fun isOverloadCast(action: CastSpell, cardDef: com.wingedsheep.sdk.model.CardDefinition): Boolean =
    action.useAlternativeCost &&
        action.altAllows(AlternativeCostType.OVERLOAD) &&
        cardDef.keywordAbilities.any { it is KeywordAbility.Overload }

/**
 * Every optional-additional-cost keyword on the card under the slot this cast declared, in printed
 * order — the costs [CastSpell.declaredCostIndices] indexes into. Empty when the cast declared no
 * slot or the card has no keyword for it.
 */
internal fun slotOptionalCosts(
    action: CastSpell,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?,
): List<KeywordAbility.OptionalAdditionalCost> {
    val slot = action.declaredCostSlot ?: return emptyList()
    return cardDef?.keywordAbilities
        ?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
        ?.filter { it.declaredSlot == slot }
        ?: emptyList()
}

/**
 * The card's optional-additional-cost keywords this cast declared it pays (CR 601.2b) — the costs
 * under [CastSpell.declaredCostSlot], narrowed to [CastSpell.declaredCostIndices] when the cast
 * picked among several ("Kicker [A] and/or [B]", CR 702.33b). Empty when the cast declared none, or
 * when the card has no keyword for the declared slot (which `validate` turns into a rejection).
 * Every stage that charges the declaration — mana total, non-mana half — sums over this list, so
 * kicking with both kickers pays both.
 */
internal fun declaredOptionalCosts(
    action: CastSpell,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?,
): List<KeywordAbility.OptionalAdditionalCost> {
    val costs = slotOptionalCosts(action, cardDef)
    if (action.declaredCostIndices.isEmpty()) return costs
    return costs.filterIndexed { index, _ -> index in action.declaredCostIndices }
}

/**
 * The per-kicker facts this cast stamps for a card with two kicker costs (CR 702.33f) —
 * [ChoiceSlot.FIRST_KICKER] / [ChoiceSlot.SECOND_KICKER] for each kicker paid, recorded alongside
 * the cast's other cast-choice slots so "if it was kicked with its [A] kicker" reads them on the
 * stack, from a self-cast trigger, and durably on the permanent. Empty for every other cast: a
 * single kicker (or any other slot) has no per-cost identity to record.
 */
internal fun linkedKickerChoices(
    action: CastSpell,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?,
): Map<ChoiceSlot, Int> {
    if (action.declaredCostSlot != ChoiceSlot.KICKED) return emptyMap()
    val costs = slotOptionalCosts(action, cardDef)
    if (costs.size < 2) return emptyMap()
    val paid = action.declaredCostIndices.ifEmpty { costs.indices.toSet() }
    return buildMap {
        if (0 in paid) put(ChoiceSlot.FIRST_KICKER, 0)
        if (1 in paid) put(ChoiceSlot.SECOND_KICKER, 1)
    }
}

/**
 * One way a caster can declare a card's optional costs (CR 601.2b) — the [slot] declared, the
 * [costs] it pays, and the [indices] the [CastSpell] carries as `declaredCostIndices` (empty when
 * the slot's costs aren't chosen among).
 */
internal data class OptionalCostDeclaration(
    val slot: ChoiceSlot,
    val costs: List<KeywordAbility.OptionalAdditionalCost>,
    val indices: Set<Int>,
)

/**
 * Every declaration the legal-action enumerators offer for a card's optional costs: one per slot
 * (kicker vs. bargain vs. teamwork, never conflated), except that a card listing several kicker
 * costs — "Kicker [A] and/or [B]" (CR 702.33b) — offers each non-empty combination, because each
 * kicker may be paid independently and the spell is kicked if any is (CR 702.33d). For two kickers
 * that is `{A}`, `{B}`, `{A, B}`, in that order.
 */
internal fun optionalCostDeclarations(
    optionalCosts: List<KeywordAbility.OptionalAdditionalCost>,
): List<OptionalCostDeclaration> =
    optionalCosts.groupBy { it.declaredSlot }.flatMap { (slot, slotCosts) ->
        if (slot != ChoiceSlot.KICKED || slotCosts.size < 2) {
            listOf(OptionalCostDeclaration(slot, slotCosts, emptySet()))
        } else {
            (1 until (1 shl slotCosts.size))
                .map { mask -> slotCosts.indices.filter { mask and (1 shl it) != 0 }.toSet() }
                .sortedWith(compareBy({ it.size }, { it.min() }))
                .map { indices ->
                    OptionalCostDeclaration(slot, slotCosts.filterIndexed { i, _ -> i in indices }, indices)
                }
        }
    }

/**
 * The mana half of [costs] each paid [times] times over, summed — "kicked with both" pays both
 * kickers' mana (CR 702.33b). Null when none of them has a mana half.
 */
internal fun optionalCostsManaPaid(
    costs: List<KeywordAbility.OptionalAdditionalCost>,
    times: Int,
): com.wingedsheep.sdk.core.ManaCost? =
    costs.mapNotNull { it.manaCostPaid(times) }
        .reduceOrNull { a, b -> com.wingedsheep.sdk.core.ManaCost(a.symbols + b.symbols) }

/**
 * The non-mana half of [costs] each paid [times] times over — "kicked with both" owes both
 * (CR 702.33b). A single half stays a bare cost rather than a one-step composite; null when none
 * of them has one.
 */
internal fun optionalCostsAdditionalPaid(
    costs: List<KeywordAbility.OptionalAdditionalCost>,
    times: Int,
): com.wingedsheep.sdk.scripting.AdditionalCost? {
    val halves = costs.mapNotNull { if (it.additionalCost != null) it.additionalCostPaid(times) else null }
    return if (halves.size <= 1) halves.firstOrNull() else com.wingedsheep.sdk.scripting.AdditionalCost.Composite(halves)
}

/**
 * The cast-variant label for a chosen subset of a two-kicker card's kickers — "Kicked {G}",
 * "Kicked {1}{U}", "Kicked {G} + {1}{U}" — so the player sees which kicker each option pays.
 */
internal fun chosenKickersLabel(costs: List<KeywordAbility.OptionalAdditionalCost>): String =
    "Kicked " + costs.joinToString(" + ") { it.manaCost?.toString() ?: it.additionalCost?.description.orEmpty() }
