package com.wingedsheep.sdk.scripting.predicates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Predicates for matching game state properties (runtime characteristics).
 * These predicates check properties that can change during the game.
 *
 * StatePredicates are composed into GameObjectFilter for use in effects, targeting, and counting.
 */
@Serializable
sealed interface StatePredicate {
    val description: String

    // =============================================================================
    // Tap State Predicates
    // =============================================================================

    @SerialName("IsTapped")
    @Serializable
    data object IsTapped : StatePredicate {
        override val description: String = "tapped"
    }

    @SerialName("IsUntapped")
    @Serializable
    data object IsUntapped : StatePredicate {
        override val description: String = "untapped"
    }

    // =============================================================================
    // Combat Predicates
    // =============================================================================

    @SerialName("IsAttacking")
    @Serializable
    data object IsAttacking : StatePredicate {
        override val description: String = "attacking"
    }

    @SerialName("IsBlocking")
    @Serializable
    data object IsBlocking : StatePredicate {
        override val description: String = "blocking"
    }

    /** Creature that is being blocked (has at least one blocker) */
    @SerialName("IsBlocked")
    @Serializable
    data object IsBlocked : StatePredicate {
        override val description: String = "blocked"
    }

    /** Creature that is attacking and has no blockers */
    @SerialName("IsUnblocked")
    @Serializable
    data object IsUnblocked : StatePredicate {
        override val description: String = "unblocked"
    }

    // =============================================================================
    // Summoning Sickness
    // =============================================================================

    /** Entered the battlefield this turn (has summoning sickness if creature) */
    @SerialName("EnteredThisTurn")
    @Serializable
    data object EnteredThisTurn : StatePredicate {
        override val description: String = "entered the battlefield this turn"
    }

    // =============================================================================
    // Damage State
    // =============================================================================

    /** Has been dealt damage this turn */
    @SerialName("WasDealtDamageThisTurn")
    @Serializable
    data object WasDealtDamageThisTurn : StatePredicate {
        override val description: String = "was dealt damage this turn"
    }

    /** Has dealt damage (ever, since entering the battlefield) */
    @SerialName("HasDealtDamage")
    @Serializable
    data object HasDealtDamage : StatePredicate {
        override val description: String = "has dealt damage"
    }

    /** Has dealt combat damage to a player (ever, since entering the battlefield) */
    @SerialName("HasDealtCombatDamageToPlayer")
    @Serializable
    data object HasDealtCombatDamageToPlayer : StatePredicate {
        override val description: String = "has dealt combat damage to a player"
    }

    // =============================================================================
    // Face-Down State
    // =============================================================================

    /** Is face-down (morph, manifest) */
    @SerialName("IsFaceDown")
    @Serializable
    data object IsFaceDown : StatePredicate {
        override val description: String = "face-down"
    }

    /** Is face-up (not face-down) */
    @SerialName("IsFaceUp")
    @Serializable
    data object IsFaceUp : StatePredicate {
        override val description: String = "face-up"
    }

    /** Has a morph ability (has MorphDataComponent) */
    @SerialName("HasMorphAbility")
    @Serializable
    data object HasMorphAbility : StatePredicate {
        override val description: String = "with a morph ability"
    }

    // =============================================================================
    // Counter Predicates
    // =============================================================================

    /** Has a counter of the specified type */
    @SerialName("HasCounter")
    @Serializable
    data class HasCounter(val counterType: String) : StatePredicate {
        override val description: String = "with a $counterType counter"
    }

    /** Has any counter of any type */
    @SerialName("HasAnyCounter")
    @Serializable
    data object HasAnyCounter : StatePredicate {
        override val description: String = "with counters"
    }

    // =============================================================================
    // Relative Power Predicates
    // =============================================================================

    /** Has the greatest power among creatures its controller controls */
    @SerialName("HasGreatestPower")
    @Serializable
    data object HasGreatestPower : StatePredicate {
        override val description: String = "with the greatest power"
    }

    // =============================================================================
    // Equipment State
    // =============================================================================

    /** Has at least one Equipment attached */
    @SerialName("IsEquipped")
    @Serializable
    data object IsEquipped : StatePredicate {
        override val description: String = "equipped"
    }

    // =============================================================================
    // Composite / Logical Combinators
    // =============================================================================

    @SerialName("StateOr")
    @Serializable
    data class Or(val predicates: List<StatePredicate>) : StatePredicate {
        override val description: String = predicates.joinToString(" or ") { it.description }
    }

    @SerialName("StateAnd")
    @Serializable
    data class And(val predicates: List<StatePredicate>) : StatePredicate {
        override val description: String = predicates.joinToString(" and ") { it.description }
    }

    @SerialName("StateNot")
    @Serializable
    data class Not(val predicate: StatePredicate) : StatePredicate {
        override val description: String = "not ${predicate.description}"
    }
}
