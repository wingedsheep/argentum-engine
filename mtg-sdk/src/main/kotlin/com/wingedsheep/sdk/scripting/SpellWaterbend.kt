package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * A spell-level **waterbend** additional cost (Avatar: The Last Airbender):
 * *"As an additional cost to cast this spell, [you may] waterbend {N}."*
 *
 * Waterbend {N} is an additional {N} generic mana cost; while paying it the caster may tap
 * untapped artifacts and/or creatures they control, each paying for {1} generic (a generic-only
 * convoke+improvise). It mirrors the activated-ability waterbend cost ([ActivatedAbility.hasWaterbend])
 * but applies to a spell, and the chosen tap permanents travel in
 * [AlternativePaymentChoice.tapForGenericPermanents] on the cast action — only the *uncovered* part of
 * the waterbend amount (`N` minus the number of permanents tapped) is owed as real mana, which keeps
 * the tap payment bounded to the waterbend cost and never to the spell's own generic.
 *
 * Three shapes, all expressed here:
 *  - **Mandatory fixed** — `waterbend {N}` ([optional] = false, [isX] = false). The {N} is always
 *    part of the cost (e.g. Benevolent River Spirit).
 *  - **Optional fixed** — `you may waterbend {N}` ([optional] = true). The caster chooses whether to
 *    pay the {N}; when paid the engine stamps [ChoiceSlot.WATERBEND_PAID] so the effect can branch via
 *    [com.wingedsheep.sdk.scripting.conditions.WaterbendWasPaid] (e.g. Ruinous Waterbending,
 *    Spirit Water Revival).
 *  - **Variable** — `waterbend {X}` ([isX] = true). The amount is the X declared at cast time
 *    (`CastSpell.xValue`); that same X feeds the spell's effect through the normal X readers
 *    (e.g. Crashing Wave, Foggy Swamp Visions). Mutually exclusive with an `{X}` in the printed
 *    mana cost — both would write the same cast X slot.
 *
 * @property amount The fixed waterbend amount N. Ignored when [isX].
 * @property optional "you may waterbend" — the cost is elective.
 * @property isX "waterbend {X}" — the amount is the cast-time X.
 */
@Serializable
data class SpellWaterbendCost(
    val amount: Int = 0,
    val optional: Boolean = false,
    val isX: Boolean = false
) {
    /** The waterbend amount as it reads in oracle text — `{X}` for the variable shape, else `{N}`. */
    val amountText: String get() = if (isX) "{X}" else "{$amount}"

    val description: String
        get() = buildString {
            append("As an additional cost to cast this spell, ")
            if (optional) append("you may ")
            append("waterbend ")
            append(amountText)
        }
}
