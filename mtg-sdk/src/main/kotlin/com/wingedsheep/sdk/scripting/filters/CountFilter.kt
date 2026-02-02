package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Filter for counting cards/permanents.
 * This is the universal filter used in CountInZone and similar.
 *
 * @deprecated Use [GameObjectFilter] instead. GameObjectFilter provides composable
 * predicate-based filtering that supports both card properties and game state.
 *
 * Migration examples:
 * - `CountFilter.Any` -> `GameObjectFilter.Any`
 * - `CountFilter.Creatures` -> `GameObjectFilter.Creature`
 * - `CountFilter.TappedCreatures` -> `GameObjectFilter.Creature.tapped()`
 * - `CountFilter.AttackingCreatures` -> `GameObjectFilter.Creature.attacking()`
 * - `CountFilter.Lands` -> `GameObjectFilter.Land`
 * - `CountFilter.CreatureColor(Color.RED)` -> `GameObjectFilter.Creature.withColor(Color.RED)`
 *
 * Use `CountFilter.toUnified()` extension function for automatic conversion.
 *
 * @see GameObjectFilter
 * @see toUnified
 */
@Deprecated(
    message = "Use GameObjectFilter instead for composable predicate-based filtering",
    replaceWith = ReplaceWith("GameObjectFilter", "com.wingedsheep.sdk.scripting.GameObjectFilter")
)
@Serializable
sealed interface CountFilter {
    val description: String

    @Serializable
    data object Any : CountFilter {
        override val description: String = ""
    }

    @Serializable
    data object Creatures : CountFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object TappedCreatures : CountFilter {
        override val description: String = "tapped creature"
    }

    @Serializable
    data object UntappedCreatures : CountFilter {
        override val description: String = "untapped creature"
    }

    @Serializable
    data object Lands : CountFilter {
        override val description: String = "land"
    }

    @Serializable
    data class LandType(val landType: String) : CountFilter {
        override val description: String = landType
    }

    @Serializable
    data class CreatureColor(val color: Color) : CountFilter {
        override val description: String = "${color.displayName.lowercase()} creature"
    }

    @Serializable
    data class CardColor(val color: Color) : CountFilter {
        override val description: String = "${color.displayName.lowercase()} card"
    }

    @Serializable
    data class HasSubtype(val subtype: String) : CountFilter {
        override val description: String = subtype
    }

    @Serializable
    data object AttackingCreatures : CountFilter {
        override val description: String = "attacking creature"
    }

    /**
     * Combine multiple filters with AND logic.
     */
    @Serializable
    data class And(val filters: List<CountFilter>) : CountFilter {
        override val description: String = filters.joinToString(" ") { it.description }
    }

    /**
     * Combine multiple filters with OR logic.
     */
    @Serializable
    data class Or(val filters: List<CountFilter>) : CountFilter {
        override val description: String = filters.joinToString(" or ") { it.description }
    }
}
