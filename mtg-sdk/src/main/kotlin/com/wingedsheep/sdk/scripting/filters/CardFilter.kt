package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Filter for matching cards during search effects.
 */
@Serializable
sealed interface CardFilter {
    val description: String

    /** Match any card */
    @Serializable
    data object AnyCard : CardFilter {
        override val description: String = "card"
    }

    /** Match creature cards */
    @Serializable
    data object CreatureCard : CardFilter {
        override val description: String = "creature card"
    }

    /** Match land cards */
    @Serializable
    data object LandCard : CardFilter {
        override val description: String = "land card"
    }

    /** Match basic land cards */
    @Serializable
    data object BasicLandCard : CardFilter {
        override val description: String = "basic land card"
    }

    /** Match sorcery cards */
    @Serializable
    data object SorceryCard : CardFilter {
        override val description: String = "sorcery card"
    }

    /** Match instant cards */
    @Serializable
    data object InstantCard : CardFilter {
        override val description: String = "instant card"
    }

    /** Match cards with a specific subtype (e.g., "Forest", "Elf") */
    @Serializable
    data class HasSubtype(val subtype: String) : CardFilter {
        override val description: String = subtype
    }

    /** Match cards with a specific color */
    @Serializable
    data class HasColor(val color: Color) : CardFilter {
        override val description: String = "${color.displayName.lowercase()} card"
    }

    /** Match cards that are both a type and have a specific property */
    @Serializable
    data class And(val filters: List<CardFilter>) : CardFilter {
        override val description: String = filters.joinToString(" ") { it.description }
    }

    /** Match cards that match any of the filters */
    @Serializable
    data class Or(val filters: List<CardFilter>) : CardFilter {
        override val description: String = filters.joinToString(" or ") { it.description }
    }

    /** Match permanent cards (creature, artifact, enchantment, planeswalker) */
    @Serializable
    data object PermanentCard : CardFilter {
        override val description: String = "permanent card"
    }

    /** Match nonland permanent cards */
    @Serializable
    data object NonlandPermanentCard : CardFilter {
        override val description: String = "nonland permanent card"
    }

    /** Match cards with mana value at most X */
    @Serializable
    data class ManaValueAtMost(val maxManaValue: Int) : CardFilter {
        override val description: String = "card with mana value $maxManaValue or less"
    }

    /** Negation filter - match cards that don't match the inner filter */
    @Serializable
    data class Not(val filter: CardFilter) : CardFilter {
        override val description: String = "non${filter.description}"
    }
}
