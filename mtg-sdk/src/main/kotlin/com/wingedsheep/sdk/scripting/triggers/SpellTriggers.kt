package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Card Drawing Triggers
// =============================================================================

/**
 * Triggers when a player draws a card.
 * "Whenever you draw a card..." or "Whenever a player draws a card..."
 */
@Serializable
data class OnDraw(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "Whenever you draw a card"
    } else {
        "Whenever a player draws a card"
    }
}

// =============================================================================
// Spell Cast Triggers
// =============================================================================

/**
 * Triggers when a spell is cast.
 * "Whenever you cast a spell..." or "Whenever a player casts a spell..."
 */
@Serializable
data class OnSpellCast(
    val controllerOnly: Boolean = true,
    val spellType: SpellTypeFilter = SpellTypeFilter.ANY,
    val manaValueAtLeast: Int? = null,
    val manaValueAtMost: Int? = null,
    val manaValueEquals: Int? = null
) : Trigger {
    override val description: String = buildString {
        append(if (controllerOnly) "Whenever you cast " else "Whenever a player casts ")
        append(when (spellType) {
            SpellTypeFilter.ANY -> "a spell"
            SpellTypeFilter.CREATURE -> "a creature spell"
            SpellTypeFilter.NONCREATURE -> "a noncreature spell"
            SpellTypeFilter.INSTANT_OR_SORCERY -> "an instant or sorcery spell"
        })
        manaValueEquals?.let { append(" with mana value $it") }
        manaValueAtLeast?.let { append(" with mana value $it or greater") }
        manaValueAtMost?.let { append(" with mana value $it or less") }
    }
}

/**
 * Filter for spell types in triggers.
 */
@Serializable
enum class SpellTypeFilter {
    ANY,
    CREATURE,
    NONCREATURE,
    INSTANT_OR_SORCERY
}

// =============================================================================
// Cycling Triggers
// =============================================================================

/**
 * Triggers when a player cycles a card.
 * "Whenever you cycle a card..." or "Whenever a player cycles a card..."
 */
@Serializable
data class OnCycle(
    val controllerOnly: Boolean = false
) : Trigger {
    override val description: String = if (controllerOnly) {
        "Whenever you cycle a card"
    } else {
        "Whenever a player cycles a card"
    }
}
