package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Stack Effects
// =============================================================================

/**
 * Counter target spell.
 * "Counter target spell."
 *
 * The countered spell is removed from the stack and placed in its owner's
 * graveyard without resolving (no effects happen).
 */
@Serializable
data object CounterSpellEffect : Effect {
    override val description: String = "Counter target spell"
}

/**
 * Counter target spell that matches a filter.
 * Used for Mystic Denial: "Counter target creature or sorcery spell."
 */
@Serializable
data class CounterSpellWithFilterEffect(
    val filter: TargetFilter = TargetFilter.SpellOnStack
) : Effect {
    override val description: String = "Counter target ${filter.baseFilter.description} spell"
}
