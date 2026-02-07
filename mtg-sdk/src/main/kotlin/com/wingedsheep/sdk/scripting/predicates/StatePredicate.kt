package com.wingedsheep.sdk.scripting

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

    @Serializable
    data object IsTapped : StatePredicate {
        override val description: String = "tapped"
    }

    @Serializable
    data object IsUntapped : StatePredicate {
        override val description: String = "untapped"
    }

    // =============================================================================
    // Combat Predicates
    // =============================================================================

    @Serializable
    data object IsAttacking : StatePredicate {
        override val description: String = "attacking"
    }

    @Serializable
    data object IsBlocking : StatePredicate {
        override val description: String = "blocking"
    }

    @Serializable
    data object IsAttackingOrBlocking : StatePredicate {
        override val description: String = "attacking or blocking"
    }

    /** Creature that is being blocked (has at least one blocker) */
    @Serializable
    data object IsBlocked : StatePredicate {
        override val description: String = "blocked"
    }

    /** Creature that is attacking and has no blockers */
    @Serializable
    data object IsUnblocked : StatePredicate {
        override val description: String = "unblocked"
    }

    // =============================================================================
    // Summoning Sickness
    // =============================================================================

    /** Entered the battlefield this turn (has summoning sickness if creature) */
    @Serializable
    data object EnteredThisTurn : StatePredicate {
        override val description: String = "entered the battlefield this turn"
    }

    // =============================================================================
    // Damage State
    // =============================================================================

    /** Has been dealt damage this turn */
    @Serializable
    data object WasDealtDamageThisTurn : StatePredicate {
        override val description: String = "was dealt damage this turn"
    }

    /** Has dealt damage (ever, since entering the battlefield) */
    @Serializable
    data object HasDealtDamage : StatePredicate {
        override val description: String = "has dealt damage"
    }

    /** Has dealt combat damage to a player (ever, since entering the battlefield) */
    @Serializable
    data object HasDealtCombatDamageToPlayer : StatePredicate {
        override val description: String = "has dealt combat damage to a player"
    }

    // =============================================================================
    // Face-Down State
    // =============================================================================

    /** Is face-down (morph, manifest) */
    @Serializable
    data object IsFaceDown : StatePredicate {
        override val description: String = "face-down"
    }

    /** Has a morph ability (has MorphDataComponent) */
    @Serializable
    data object HasMorphAbility : StatePredicate {
        override val description: String = "with a morph ability"
    }
}
