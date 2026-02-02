package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Filter for spell types that can be countered.
 *
 * @deprecated Use [TargetFilter] with [Zone.Stack] instead. TargetFilter provides
 * composable predicate-based filtering with zone context.
 *
 * Migration examples:
 * - `SpellFilter.AnySpell` -> `TargetFilter.SpellOnStack`
 * - `SpellFilter.CreatureSpell` -> `TargetFilter.CreatureSpellOnStack`
 * - `SpellFilter.NonCreatureSpell` -> `TargetFilter.NoncreatureSpellOnStack`
 * - `SpellFilter.InstantSpell` -> `TargetFilter(GameObjectFilter.Instant, zone = Zone.Stack)`
 * - `SpellFilter.SorcerySpell` -> `TargetFilter(GameObjectFilter.Sorcery, zone = Zone.Stack)`
 *
 * Use `SpellFilter.toUnified()` extension function for automatic conversion.
 *
 * @see TargetFilter
 * @see GameObjectFilter
 * @see toUnified
 */
@Deprecated(
    message = "Use TargetFilter with Zone.Stack instead for composable predicate-based filtering",
    replaceWith = ReplaceWith("TargetFilter", "com.wingedsheep.sdk.scripting.TargetFilter")
)
@Serializable
sealed interface SpellFilter {
    val description: String

    @Serializable
    data object AnySpell : SpellFilter {
        override val description: String = ""
    }

    @Serializable
    data object CreatureSpell : SpellFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object NonCreatureSpell : SpellFilter {
        override val description: String = "noncreature"
    }

    @Serializable
    data object SorcerySpell : SpellFilter {
        override val description: String = "sorcery"
    }

    @Serializable
    data object InstantSpell : SpellFilter {
        override val description: String = "instant"
    }

    @Serializable
    data class CreatureOrSorcery(val dummy: Unit = Unit) : SpellFilter {
        override val description: String = "creature or sorcery"
    }
}
