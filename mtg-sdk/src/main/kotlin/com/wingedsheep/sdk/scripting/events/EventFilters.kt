package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
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

    @Serializable
    data object Any : RecipientFilter {
        override val description = "any target"
    }

    @Serializable
    data object You : RecipientFilter {
        override val description = "you"
    }

    @Serializable
    data object Opponent : RecipientFilter {
        override val description = "an opponent"
    }

    @Serializable
    data object AnyPlayer : RecipientFilter {
        override val description = "a player"
    }

    @Serializable
    data object CreatureYouControl : RecipientFilter {
        override val description = "a creature you control"
    }

    @Serializable
    data object CreatureOpponentControls : RecipientFilter {
        override val description = "a creature an opponent controls"
    }

    @Serializable
    data object AnyCreature : RecipientFilter {
        override val description = "a creature"
    }

    @Serializable
    data object PermanentYouControl : RecipientFilter {
        override val description = "a permanent you control"
    }

    @Serializable
    data object AnyPermanent : RecipientFilter {
        override val description = "a permanent"
    }

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

    @Serializable
    data object Any : SourceFilter {
        override val description = "any source"
    }

    @Serializable
    data object Combat : SourceFilter {
        override val description = "combat"
    }

    @Serializable
    data object NonCombat : SourceFilter {
        override val description = "a non-combat source"
    }

    @Serializable
    data object Spell : SourceFilter {
        override val description = "a spell"
    }

    @Serializable
    data object Ability : SourceFilter {
        override val description = "an ability"
    }

    @Serializable
    data class HasColor(val color: Color) : SourceFilter {
        override val description = "a ${color.name.lowercase()} source"
    }

    @Serializable
    data class HasType(val type: String) : SourceFilter {
        override val description = "a $type"
    }

    @Serializable
    data class Matching(val filter: GameObjectFilter) : SourceFilter {
        override val description = filter.description
    }
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

    @Serializable
    data object Any : DamageType {
        override val description = ""
    }

    @Serializable
    data object Combat : DamageType {
        override val description = "combat"
    }

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

    @Serializable
    data object Any : CounterTypeFilter {
        override val description = ""
    }

    @Serializable
    data object PlusOnePlusOne : CounterTypeFilter {
        override val description = "+1/+1"
    }

    @Serializable
    data object MinusOneMinusOne : CounterTypeFilter {
        override val description = "-1/-1"
    }

    @Serializable
    data object Loyalty : CounterTypeFilter {
        override val description = "loyalty"
    }

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

    @Serializable
    data object You : ControllerFilter {
        override val description = "under your control"
    }

    @Serializable
    data object Opponent : ControllerFilter {
        override val description = "under an opponent's control"
    }

    @Serializable
    data object Any : ControllerFilter {
        override val description = ""
    }
}

