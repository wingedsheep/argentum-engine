package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.Serializable

/**
 * Predicates for matching card properties (static characteristics).
 * These predicates check inherent card properties that don't change based on game state.
 *
 * CardPredicates are composed into GameObjectFilter for use in effects, targeting, and counting.
 */
@Serializable
sealed interface CardPredicate {
    val description: String

    // =============================================================================
    // Type Predicates
    // =============================================================================

    @Serializable
    data object IsCreature : CardPredicate {
        override val description: String = "creature"
    }

    @Serializable
    data object IsLand : CardPredicate {
        override val description: String = "land"
    }

    @Serializable
    data object IsArtifact : CardPredicate {
        override val description: String = "artifact"
    }

    @Serializable
    data object IsEnchantment : CardPredicate {
        override val description: String = "enchantment"
    }

    @Serializable
    data object IsPlaneswalker : CardPredicate {
        override val description: String = "planeswalker"
    }

    @Serializable
    data object IsInstant : CardPredicate {
        override val description: String = "instant"
    }

    @Serializable
    data object IsSorcery : CardPredicate {
        override val description: String = "sorcery"
    }

    @Serializable
    data object IsBasicLand : CardPredicate {
        override val description: String = "basic land"
    }

    /** Matches creature, artifact, enchantment, planeswalker, land */
    @Serializable
    data object IsPermanent : CardPredicate {
        override val description: String = "permanent"
    }

    @Serializable
    data object IsNonland : CardPredicate {
        override val description: String = "nonland"
    }

    @Serializable
    data object IsNoncreature : CardPredicate {
        override val description: String = "noncreature"
    }

    @Serializable
    data object IsToken : CardPredicate {
        override val description: String = "token"
    }

    @Serializable
    data object IsNontoken : CardPredicate {
        override val description: String = "nontoken"
    }

    // =============================================================================
    // Color Predicates
    // =============================================================================

    @Serializable
    data class HasColor(val color: Color) : CardPredicate {
        override val description: String = color.displayName.lowercase()
    }

    @Serializable
    data class NotColor(val color: Color) : CardPredicate {
        override val description: String = "non${color.displayName.lowercase()}"
    }

    @Serializable
    data object IsColorless : CardPredicate {
        override val description: String = "colorless"
    }

    @Serializable
    data object IsMulticolored : CardPredicate {
        override val description: String = "multicolored"
    }

    @Serializable
    data object IsMonocolored : CardPredicate {
        override val description: String = "monocolored"
    }

    // =============================================================================
    // Subtype Predicates
    // =============================================================================

    @Serializable
    data class HasSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = subtype.value
    }

    @Serializable
    data class NotSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = "non-${subtype.value}"
    }

    /** Matches basic land types: Plains, Island, Swamp, Mountain, Forest */
    @Serializable
    data class HasBasicLandType(val landType: String) : CardPredicate {
        override val description: String = landType
    }

    // =============================================================================
    // Keyword Predicates
    // =============================================================================

    @Serializable
    data class HasKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "with ${keyword.displayName.lowercase()}"
    }

    @Serializable
    data class NotKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "without ${keyword.displayName.lowercase()}"
    }

    // =============================================================================
    // Mana Value Predicates
    // =============================================================================

    @Serializable
    data class ManaValueEquals(val value: Int) : CardPredicate {
        override val description: String = "with mana value $value"
    }

    @Serializable
    data class ManaValueAtMost(val max: Int) : CardPredicate {
        override val description: String = "with mana value $max or less"
    }

    @Serializable
    data class ManaValueAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with mana value $min or greater"
    }

    // =============================================================================
    // Power/Toughness Predicates
    // =============================================================================

    @Serializable
    data class PowerEquals(val value: Int) : CardPredicate {
        override val description: String = "with power $value"
    }

    @Serializable
    data class PowerAtMost(val max: Int) : CardPredicate {
        override val description: String = "with power $max or less"
    }

    @Serializable
    data class PowerAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with power $min or greater"
    }

    @Serializable
    data class ToughnessEquals(val value: Int) : CardPredicate {
        override val description: String = "with toughness $value"
    }

    @Serializable
    data class ToughnessAtMost(val max: Int) : CardPredicate {
        override val description: String = "with toughness $max or less"
    }

    @Serializable
    data class ToughnessAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with toughness $min or greater"
    }

    // =============================================================================
    // Composite Predicates
    // =============================================================================

    @Serializable
    data class And(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" ") { it.description }
    }

    @Serializable
    data class Or(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" or ") { it.description }
    }

    @Serializable
    data class Not(val predicate: CardPredicate) : CardPredicate {
        override val description: String = "non-${predicate.description}"
    }
}
