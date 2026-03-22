package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Predicates for matching card properties (static characteristics).
 * These predicates check inherent card properties that don't change based on game state.
 *
 * CardPredicates are composed into GameObjectFilter for use in effects, targeting, and counting.
 */
@Serializable
sealed interface CardPredicate : TextReplaceable<CardPredicate> {
    val description: String

    // =============================================================================
    // Type Predicates
    // =============================================================================

    @SerialName("IsCreature")
    @Serializable
    data object IsCreature : CardPredicate {
        override val description: String = "creature"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsLand")
    @Serializable
    data object IsLand : CardPredicate {
        override val description: String = "land"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsArtifact")
    @Serializable
    data object IsArtifact : CardPredicate {
        override val description: String = "artifact"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsEnchantment")
    @Serializable
    data object IsEnchantment : CardPredicate {
        override val description: String = "enchantment"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsPlaneswalker")
    @Serializable
    data object IsPlaneswalker : CardPredicate {
        override val description: String = "planeswalker"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsInstant")
    @Serializable
    data object IsInstant : CardPredicate {
        override val description: String = "instant"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsSorcery")
    @Serializable
    data object IsSorcery : CardPredicate {
        override val description: String = "sorcery"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsBasicLand")
    @Serializable
    data object IsBasicLand : CardPredicate {
        override val description: String = "basic land"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /** Matches creature, artifact, enchantment, planeswalker, land */
    @SerialName("IsPermanent")
    @Serializable
    data object IsPermanent : CardPredicate {
        override val description: String = "permanent"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsNonland")
    @Serializable
    data object IsNonland : CardPredicate {
        override val description: String = "nonland"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsNoncreature")
    @Serializable
    data object IsNoncreature : CardPredicate {
        override val description: String = "noncreature"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsNonenchantment")
    @Serializable
    data object IsNonenchantment : CardPredicate {
        override val description: String = "nonenchantment"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsToken")
    @Serializable
    data object IsToken : CardPredicate {
        override val description: String = "token"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsNontoken")
    @Serializable
    data object IsNontoken : CardPredicate {
        override val description: String = "nontoken"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Supertype Predicates
    // =============================================================================

    @SerialName("IsLegendary")
    @Serializable
    data object IsLegendary : CardPredicate {
        override val description: String = "legendary"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsNonlegendary")
    @Serializable
    data object IsNonlegendary : CardPredicate {
        override val description: String = "nonlegendary"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Color Predicates
    // =============================================================================

    @SerialName("HasColor")
    @Serializable
    data class HasColor(val color: Color) : CardPredicate {
        override val description: String = color.displayName.lowercase()
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("NotColor")
    @Serializable
    data class NotColor(val color: Color) : CardPredicate {
        override val description: String = "non${color.displayName.lowercase()}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsColorless")
    @Serializable
    data object IsColorless : CardPredicate {
        override val description: String = "colorless"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsMulticolored")
    @Serializable
    data object IsMulticolored : CardPredicate {
        override val description: String = "multicolored"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("IsMonocolored")
    @Serializable
    data object IsMonocolored : CardPredicate {
        override val description: String = "monocolored"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Subtype Predicates
    // =============================================================================

    @SerialName("HasSubtype")
    @Serializable
    data class HasSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = subtype.value
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = replacer.replaceSubtype(subtype)
            return if (new == subtype) this else HasSubtype(new)
        }
    }

    /**
     * Matches cards with any one of the given subtypes (OR logic).
     * Used for "Rabbits, Bats, Birds, and/or Mice" patterns.
     */
    @SerialName("HasAnyOfSubtypes")
    @Serializable
    data class HasAnyOfSubtypes(val subtypes: List<Subtype>) : CardPredicate {
        override val description: String = subtypes.joinToString(", ") { it.value }
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newSubtypes = subtypes.map { replacer.replaceSubtype(it) }
            return if (newSubtypes == subtypes) this else HasAnyOfSubtypes(newSubtypes)
        }
    }

    @SerialName("NotSubtype")
    @Serializable
    data class NotSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = "non-${subtype.value}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = replacer.replaceSubtype(subtype)
            return if (new == subtype) this else NotSubtype(new)
        }
    }

    /** Matches basic land types: Plains, Island, Swamp, Mountain, Forest */
    @SerialName("HasBasicLandType")
    @Serializable
    data class HasBasicLandType(val landType: String) : CardPredicate {
        override val description: String = landType
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Name Predicates
    // =============================================================================

    @SerialName("NameEquals")
    @Serializable
    data class NameEquals(val name: String) : CardPredicate {
        override val description: String = "named $name"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Keyword Predicates
    // =============================================================================

    @SerialName("HasKeyword")
    @Serializable
    data class HasKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "with ${keyword.displayName.lowercase()}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("NotKeyword")
    @Serializable
    data class NotKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "without ${keyword.displayName.lowercase()}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Mana Value Predicates
    // =============================================================================

    @SerialName("ManaValueEquals")
    @Serializable
    data class ManaValueEquals(val value: Int) : CardPredicate {
        override val description: String = "with mana value $value"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("ManaValueAtMost")
    @Serializable
    data class ManaValueAtMost(val max: Int) : CardPredicate {
        override val description: String = "with mana value $max or less"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("ManaValueAtLeast")
    @Serializable
    data class ManaValueAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with mana value $min or greater"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Power/Toughness Predicates
    // =============================================================================

    @SerialName("PowerEquals")
    @Serializable
    data class PowerEquals(val value: Int) : CardPredicate {
        override val description: String = "with power $value"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("PowerAtMost")
    @Serializable
    data class PowerAtMost(val max: Int) : CardPredicate {
        override val description: String = "with power $max or less"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("PowerAtLeast")
    @Serializable
    data class PowerAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with power $min or greater"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("ToughnessEquals")
    @Serializable
    data class ToughnessEquals(val value: Int) : CardPredicate {
        override val description: String = "with toughness $value"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("ToughnessAtMost")
    @Serializable
    data class ToughnessAtMost(val max: Int) : CardPredicate {
        override val description: String = "with toughness $max or less"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("ToughnessAtLeast")
    @Serializable
    data class ToughnessAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with toughness $min or greater"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Context-relative Predicates (Pipeline Variable References)
    // =============================================================================

    /** Matches cards that have a subtype matching a value stored in chosenValues[variableName] */
    @SerialName("HasSubtypeFromVariable")
    @Serializable
    data class HasSubtypeFromVariable(val variableName: String) : CardPredicate {
        override val description: String = "of the chosen type"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /** Matches cards that have a subtype matching any string in storedStringLists[listName] */
    @SerialName("HasSubtypeInStoredList")
    @Serializable
    data class HasSubtypeInStoredList(val listName: String) : CardPredicate {
        override val description: String = "of a type chosen this way"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Source-relative Predicates
    // =============================================================================

    /** Matches creatures that are NOT of the type chosen on the source permanent */
    @SerialName("NotOfSourceChosenType")
    @Serializable
    data object NotOfSourceChosenType : CardPredicate {
        override val description: String = "that isn't of the chosen type"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /** Matches spells that share a creature subtype with the source permanent's projected types */
    @SerialName("SharesCreatureTypeWithSource")
    @Serializable
    data object SharesCreatureTypeWithSource : CardPredicate {
        override val description: String = "that shares a creature type with this creature"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /** Matches creatures that share a creature subtype with the triggering entity */
    @SerialName("SharesCreatureTypeWithTriggeringEntity")
    @Serializable
    data object SharesCreatureTypeWithTriggeringEntity : CardPredicate {
        override val description: String = "that shares a creature type with it"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Stack Item Type Predicates
    // =============================================================================

    /**
     * Matches activated or triggered abilities on the stack (not spells).
     * Used by cards like Stifle: "Counter target activated or triggered ability."
     */
    @SerialName("IsActivatedOrTriggeredAbility")
    @Serializable
    data object IsActivatedOrTriggeredAbility : CardPredicate {
        override val description: String = "activated or triggered ability"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    // =============================================================================
    // Composite Predicates
    // =============================================================================

    @SerialName("And")
    @Serializable
    data class And(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" ") { it.description }
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = predicates.map { it.applyTextReplacement(replacer) }
            return if (new == predicates) this else And(new)
        }
    }

    @SerialName("Or")
    @Serializable
    data class Or(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" or ") { it.description }
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = predicates.map { it.applyTextReplacement(replacer) }
            return if (new == predicates) this else Or(new)
        }
    }

    @SerialName("Not")
    @Serializable
    data class Not(val predicate: CardPredicate) : CardPredicate {
        override val description: String = "non-${predicate.description}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = predicate.applyTextReplacement(replacer)
            return if (new === predicate) this else Not(new)
        }
    }
}
