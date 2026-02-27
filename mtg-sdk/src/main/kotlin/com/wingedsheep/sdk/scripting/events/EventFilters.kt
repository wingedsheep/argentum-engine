package com.wingedsheep.sdk.scripting.events

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Recipient Filters - Who/what receives damage or effects
// =============================================================================

/**
 * Filter for damage/effect recipients.
 */
@Serializable
sealed interface RecipientFilter {
    val description: String

    @SerialName("RecipientAny")
    @Serializable
    data object Any : RecipientFilter {
        override val description = "any target"
    }

    @SerialName("You")
    @Serializable
    data object You : RecipientFilter {
        override val description = "you"
    }

    @SerialName("RecipientOpponent")
    @Serializable
    data object Opponent : RecipientFilter {
        override val description = "an opponent"
    }

    @SerialName("AnyPlayer")
    @Serializable
    data object AnyPlayer : RecipientFilter {
        override val description = "a player"
    }

    @SerialName("CreatureYouControl")
    @Serializable
    data object CreatureYouControl : RecipientFilter {
        override val description = "a creature you control"
    }

    @SerialName("CreatureOpponentControls")
    @Serializable
    data object CreatureOpponentControls : RecipientFilter {
        override val description = "a creature an opponent controls"
    }

    @SerialName("AnyCreature")
    @Serializable
    data object AnyCreature : RecipientFilter {
        override val description = "a creature"
    }

    @SerialName("PermanentYouControl")
    @Serializable
    data object PermanentYouControl : RecipientFilter {
        override val description = "a permanent you control"
    }

    @SerialName("AnyPermanent")
    @Serializable
    data object AnyPermanent : RecipientFilter {
        override val description = "a permanent"
    }

    @SerialName("RecipientSelf")
    @Serializable
    data object Self : RecipientFilter {
        override val description = "this permanent"
    }

    @SerialName("RecipientEnchantedCreature")
    @Serializable
    data object EnchantedCreature : RecipientFilter {
        override val description = "enchanted creature"
    }

    @SerialName("RecipientMatching")
    @Serializable
    data class Matching(val filter: GameObjectFilter) : RecipientFilter {
        override val description = filter.description
    }
}

// =============================================================================
// Source Filters - Where damage or effects come from
// =============================================================================

/**
 * Filter for damage/effect sources.
 */
@Serializable
sealed interface SourceFilter {
    val description: String

    @SerialName("SourceAny")
    @Serializable
    data object Any : SourceFilter {
        override val description = "any source"
    }

    @SerialName("SourceCombat")
    @Serializable
    data object Combat : SourceFilter {
        override val description = "combat"
    }

    @SerialName("SourceNonCombat")
    @Serializable
    data object NonCombat : SourceFilter {
        override val description = "a non-combat source"
    }

    @SerialName("Spell")
    @Serializable
    data object Spell : SourceFilter {
        override val description = "a spell"
    }

    @SerialName("Ability")
    @Serializable
    data object Ability : SourceFilter {
        override val description = "an ability"
    }

    @SerialName("HasColor")
    @Serializable
    data class HasColor(val color: Color) : SourceFilter {
        override val description = "a ${color.name.lowercase()} source"
    }

    @SerialName("HasType")
    @Serializable
    data class HasType(val type: String) : SourceFilter {
        override val description = "a $type"
    }

    @SerialName("SourceEnchantedCreature")
    @Serializable
    data object EnchantedCreature : SourceFilter {
        override val description = "enchanted creature"
    }

    @SerialName("SourceCreature")
    @Serializable
    data object Creature : SourceFilter {
        override val description = "a creature"
    }

    @SerialName("SourceMatching")
    @Serializable
    data class Matching(val filter: GameObjectFilter) : SourceFilter {
        override val description = filter.description
    }
}

// =============================================================================
// Spell Type Filters
// =============================================================================

/**
 * Filter for spell types in triggers.
 */
@Serializable
enum class SpellTypeFilter {
    ANY,
    CREATURE,
    NONCREATURE,
    INSTANT_OR_SORCERY,
    ENCHANTMENT
}

// =============================================================================
// Damage Type - Classification of damage
// =============================================================================

/**
 * Damage type classification.
 */
@Serializable
sealed interface DamageType {
    val description: String

    @SerialName("DamageAny")
    @Serializable
    data object Any : DamageType {
        override val description = ""
    }

    @SerialName("DamageCombat")
    @Serializable
    data object Combat : DamageType {
        override val description = "combat"
    }

    @SerialName("DamageNonCombat")
    @Serializable
    data object NonCombat : DamageType {
        override val description = "noncombat"
    }
}

// =============================================================================
// Counter Type Filters
// =============================================================================

/**
 * Counter type specification.
 */
@Serializable
sealed interface CounterTypeFilter {
    val description: String

    @SerialName("CounterAny")
    @Serializable
    data object Any : CounterTypeFilter {
        override val description = ""
    }

    @SerialName("PlusOnePlusOne")
    @Serializable
    data object PlusOnePlusOne : CounterTypeFilter {
        override val description = "+1/+1"
    }

    @SerialName("MinusOneMinusOne")
    @Serializable
    data object MinusOneMinusOne : CounterTypeFilter {
        override val description = "-1/-1"
    }

    @SerialName("Loyalty")
    @Serializable
    data object Loyalty : CounterTypeFilter {
        override val description = "loyalty"
    }

    @SerialName("Named")
    @Serializable
    data class Named(val name: String) : CounterTypeFilter {
        override val description = name
    }
}

// =============================================================================
// Controller Filters
// =============================================================================

/**
 * Controller/owner filters.
 */
@Serializable
sealed interface ControllerFilter {
    val description: String

    @SerialName("ControllerYou")
    @Serializable
    data object You : ControllerFilter {
        override val description = "under your control"
    }

    @SerialName("ControllerOpponent")
    @Serializable
    data object Opponent : ControllerFilter {
        override val description = "under an opponent's control"
    }

    @SerialName("ControllerAny")
    @Serializable
    data object Any : ControllerFilter {
        override val description = ""
    }
}
