package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.SerialName
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

    @SerialName("IsCreature")
    @Serializable
    data object IsCreature : CardPredicate {
        override val description: String = "creature"
    }

    @SerialName("IsLand")
    @Serializable
    data object IsLand : CardPredicate {
        override val description: String = "land"
    }

    @SerialName("IsArtifact")
    @Serializable
    data object IsArtifact : CardPredicate {
        override val description: String = "artifact"
    }

    @SerialName("IsEnchantment")
    @Serializable
    data object IsEnchantment : CardPredicate {
        override val description: String = "enchantment"
    }

    @SerialName("IsPlaneswalker")
    @Serializable
    data object IsPlaneswalker : CardPredicate {
        override val description: String = "planeswalker"
    }

    @SerialName("IsInstant")
    @Serializable
    data object IsInstant : CardPredicate {
        override val description: String = "instant"
    }

    @SerialName("IsSorcery")
    @Serializable
    data object IsSorcery : CardPredicate {
        override val description: String = "sorcery"
    }

    @SerialName("IsBasicLand")
    @Serializable
    data object IsBasicLand : CardPredicate {
        override val description: String = "basic land"
    }

    /** Matches creature, artifact, enchantment, planeswalker, land */
    @SerialName("IsPermanent")
    @Serializable
    data object IsPermanent : CardPredicate {
        override val description: String = "permanent"
    }

    @SerialName("IsNonland")
    @Serializable
    data object IsNonland : CardPredicate {
        override val description: String = "nonland"
    }

    @SerialName("IsNoncreature")
    @Serializable
    data object IsNoncreature : CardPredicate {
        override val description: String = "noncreature"
    }

    @SerialName("IsToken")
    @Serializable
    data object IsToken : CardPredicate {
        override val description: String = "token"
    }

    @SerialName("IsNontoken")
    @Serializable
    data object IsNontoken : CardPredicate {
        override val description: String = "nontoken"
    }

    // =============================================================================
    // Color Predicates
    // =============================================================================

    @SerialName("HasColor")
    @Serializable
    data class HasColor(val color: Color) : CardPredicate {
        override val description: String = color.displayName.lowercase()
    }

    @SerialName("NotColor")
    @Serializable
    data class NotColor(val color: Color) : CardPredicate {
        override val description: String = "non${color.displayName.lowercase()}"
    }

    @SerialName("IsColorless")
    @Serializable
    data object IsColorless : CardPredicate {
        override val description: String = "colorless"
    }

    @SerialName("IsMulticolored")
    @Serializable
    data object IsMulticolored : CardPredicate {
        override val description: String = "multicolored"
    }

    @SerialName("IsMonocolored")
    @Serializable
    data object IsMonocolored : CardPredicate {
        override val description: String = "monocolored"
    }

    // =============================================================================
    // Subtype Predicates
    // =============================================================================

    @SerialName("HasSubtype")
    @Serializable
    data class HasSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = subtype.value
    }

    @SerialName("NotSubtype")
    @Serializable
    data class NotSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = "non-${subtype.value}"
    }

    /** Matches basic land types: Plains, Island, Swamp, Mountain, Forest */
    @SerialName("HasBasicLandType")
    @Serializable
    data class HasBasicLandType(val landType: String) : CardPredicate {
        override val description: String = landType
    }

    // =============================================================================
    // Name Predicates
    // =============================================================================

    @SerialName("NameEquals")
    @Serializable
    data class NameEquals(val name: String) : CardPredicate {
        override val description: String = "named $name"
    }

    // =============================================================================
    // Keyword Predicates
    // =============================================================================

    @SerialName("HasKeyword")
    @Serializable
    data class HasKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "with ${keyword.displayName.lowercase()}"
    }

    @SerialName("NotKeyword")
    @Serializable
    data class NotKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "without ${keyword.displayName.lowercase()}"
    }

    // =============================================================================
    // Mana Value Predicates
    // =============================================================================

    @SerialName("ManaValueEquals")
    @Serializable
    data class ManaValueEquals(val value: Int) : CardPredicate {
        override val description: String = "with mana value $value"
    }

    @SerialName("ManaValueAtMost")
    @Serializable
    data class ManaValueAtMost(val max: Int) : CardPredicate {
        override val description: String = "with mana value $max or less"
    }

    @SerialName("ManaValueAtLeast")
    @Serializable
    data class ManaValueAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with mana value $min or greater"
    }

    // =============================================================================
    // Power/Toughness Predicates
    // =============================================================================

    @SerialName("PowerEquals")
    @Serializable
    data class PowerEquals(val value: Int) : CardPredicate {
        override val description: String = "with power $value"
    }

    @SerialName("PowerAtMost")
    @Serializable
    data class PowerAtMost(val max: Int) : CardPredicate {
        override val description: String = "with power $max or less"
    }

    @SerialName("PowerAtLeast")
    @Serializable
    data class PowerAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with power $min or greater"
    }

    @SerialName("ToughnessEquals")
    @Serializable
    data class ToughnessEquals(val value: Int) : CardPredicate {
        override val description: String = "with toughness $value"
    }

    @SerialName("ToughnessAtMost")
    @Serializable
    data class ToughnessAtMost(val max: Int) : CardPredicate {
        override val description: String = "with toughness $max or less"
    }

    @SerialName("ToughnessAtLeast")
    @Serializable
    data class ToughnessAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with toughness $min or greater"
    }

    // =============================================================================
    // Source-relative Predicates
    // =============================================================================

    /** Matches creatures that are NOT of the type chosen on the source permanent */
    @SerialName("NotOfSourceChosenType")
    @Serializable
    data object NotOfSourceChosenType : CardPredicate {
        override val description: String = "that isn't of the chosen type"
    }

    /** Matches spells that share a creature subtype with the source permanent's projected types */
    @SerialName("SharesCreatureTypeWithSource")
    @Serializable
    data object SharesCreatureTypeWithSource : CardPredicate {
        override val description: String = "that shares a creature type with this creature"
    }

    // =============================================================================
    // Composite Predicates
    // =============================================================================

    @SerialName("And")
    @Serializable
    data class And(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" ") { it.description }
    }

    @SerialName("Or")
    @Serializable
    data class Or(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" or ") { it.description }
    }

    @SerialName("Not")
    @Serializable
    data class Not(val predicate: CardPredicate) : CardPredicate {
        override val description: String = "non-${predicate.description}"
    }
}
