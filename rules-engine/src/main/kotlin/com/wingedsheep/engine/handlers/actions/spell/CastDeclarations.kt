package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
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
 * The card's optional-additional-cost keywords matching the slot this cast declared (CR 601.2b) —
 * empty when the cast declared none, or when the card has no keyword for the declared slot (which
 * `validate` turns into a rejection). A card can carry two entries for one slot (a mana kicker
 * alongside a sacrifice kicker), hence a list: the mana portion and the non-mana portion are read
 * separately.
 */
internal fun declaredOptionalCosts(
    action: CastSpell,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?,
): List<KeywordAbility.OptionalAdditionalCost> {
    val slot = action.declaredCostSlot ?: return emptyList()
    return cardDef?.keywordAbilities
        ?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
        ?.filter { it.declaredSlot == slot }
        ?: emptyList()
}
