package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.Serializable

/**
 * Which mana a cast permission lets its holder spend **as though it were mana of any color**
 * (CR 609.4b) — the narrow siblings of the blanket `withAnyManaColor` rider, which relaxes every
 * colored pip of the cost for all mana alike.
 *
 * These don't rewrite the cost: every colored requirement (colored, hybrid, Phyrexian,
 * monocolored-hybrid pips) additionally accepts the chosen kind of mana, while `{C}`, `{S}` and
 * generic pips are untouched. Only the mana the value names stands in for a color; any other mana
 * still has to match the pip.
 */
@Serializable
enum class ManaSpentAsAnyColor(val description: String) {
    /**
     * "You may spend colorless mana as though it were mana of any color to cast that spell"
     * (Abstruse Appropriation). Colored mana still can't pay `{C}`.
     */
    COLORLESS("colorless mana"),

    /**
     * "You may spend mana from snow sources as though it were mana of any color to cast those
     * spells" (Draugr Necromancer). Any mana a snow source produced — of any color, or colorless
     * (CR 107.4h's snow mana) — may pay a colored pip.
     */
    SNOW("mana from snow sources"),
}
