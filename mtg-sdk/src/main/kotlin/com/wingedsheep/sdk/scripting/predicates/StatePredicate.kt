package com.wingedsheep.sdk.scripting.predicates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Predicates for matching game state properties (runtime characteristics).
 * These predicates check properties that can change during the game.
 *
 * StatePredicates are composed into GameObjectFilter for use in effects, targeting, and counting.
 *
 * Variants split into two groups so every evaluator can exhaustively dispatch over them:
 *  - [Entity] — properties read from the entity's current projected state (tap, combat,
 *    face-down, counters, equipment, power, morph, ETB-this-turn).
 *  - [History] — accumulated turn-history facts recorded on the entity (damage-dealt,
 *    damage-received).
 *
 * Combinators (`Or` / `And` / `Not`) live at the root so they can mix Entity and History.
 */
@Serializable
sealed interface StatePredicate {
    val description: String

    /** Predicates that read the entity's current (projected) state. */
    @Serializable
    sealed interface Entity : StatePredicate

    /** Predicates that read accumulated turn-history facts on the entity. */
    @Serializable
    sealed interface History : StatePredicate

    // =============================================================================
    // Tap State (Entity)
    // =============================================================================

    @SerialName("IsTapped")
    @Serializable
    data object IsTapped : Entity {
        override val description: String = "tapped"
    }

    @SerialName("IsUntapped")
    @Serializable
    data object IsUntapped : Entity {
        override val description: String = "untapped"
    }

    // =============================================================================
    // Combat (Entity)
    // =============================================================================

    @SerialName("IsAttacking")
    @Serializable
    data object IsAttacking : Entity {
        override val description: String = "attacking"
    }

    @SerialName("IsBlocking")
    @Serializable
    data object IsBlocking : Entity {
        override val description: String = "blocking"
    }

    /** Creature that is being blocked (has at least one blocker) */
    @SerialName("IsBlocked")
    @Serializable
    data object IsBlocked : Entity {
        override val description: String = "blocked"
    }

    /** Creature that is attacking and has no blockers */
    @SerialName("IsUnblocked")
    @Serializable
    data object IsUnblocked : Entity {
        override val description: String = "unblocked"
    }

    // =============================================================================
    // Summoning Sickness (Entity)
    // =============================================================================

    /** Entered the battlefield this turn (has summoning sickness if creature) */
    @SerialName("EnteredThisTurn")
    @Serializable
    data object EnteredThisTurn : Entity {
        override val description: String = "entered the battlefield this turn"
    }

    // =============================================================================
    // Damage History (History)
    // =============================================================================

    /** Has been dealt damage this turn */
    @SerialName("WasDealtDamageThisTurn")
    @Serializable
    data object WasDealtDamageThisTurn : History {
        override val description: String = "was dealt damage this turn"
    }

    /** Has dealt damage (ever, since entering the battlefield) */
    @SerialName("HasDealtDamage")
    @Serializable
    data object HasDealtDamage : History {
        override val description: String = "has dealt damage"
    }

    /** Has dealt combat damage to a player (ever, since entering the battlefield) */
    @SerialName("HasDealtCombatDamageToPlayer")
    @Serializable
    data object HasDealtCombatDamageToPlayer : History {
        override val description: String = "has dealt combat damage to a player"
    }

    // =============================================================================
    // Face-Down State (Entity)
    // =============================================================================

    /** Is face-down (morph, manifest) */
    @SerialName("IsFaceDown")
    @Serializable
    data object IsFaceDown : Entity {
        override val description: String = "face-down"
    }

    /** Is face-up (not face-down) */
    @SerialName("IsFaceUp")
    @Serializable
    data object IsFaceUp : Entity {
        override val description: String = "face-up"
    }

    /** Has a morph ability (has MorphDataComponent) */
    @SerialName("HasMorphAbility")
    @Serializable
    data object HasMorphAbility : Entity {
        override val description: String = "with a morph ability"
    }

    // =============================================================================
    // Counters (Entity)
    // =============================================================================

    /** Has a counter of the specified type */
    @SerialName("HasCounter")
    @Serializable
    data class HasCounter(val counterType: String) : Entity {
        override val description: String = "with a $counterType counter"
    }

    /** Has any counter of any type */
    @SerialName("HasAnyCounter")
    @Serializable
    data object HasAnyCounter : Entity {
        override val description: String = "with counters"
    }

    // =============================================================================
    // Relative Power (Entity)
    // =============================================================================

    /** Has the greatest power among creatures its controller controls */
    @SerialName("HasGreatestPower")
    @Serializable
    data object HasGreatestPower : Entity {
        override val description: String = "with the greatest power"
    }

    // =============================================================================
    // Equipment (Entity)
    // =============================================================================

    /** Has at least one Equipment attached */
    @SerialName("IsEquipped")
    @Serializable
    data object IsEquipped : Entity {
        override val description: String = "equipped"
    }

    /** Has an Equipment attached, an Aura attached, or any counter (MTG "modified" definition) */
    @SerialName("IsModified")
    @Serializable
    data object IsModified : Entity {
        override val description: String = "modified"
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
