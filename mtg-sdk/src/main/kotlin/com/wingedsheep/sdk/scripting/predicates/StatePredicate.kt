package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.CardType
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

    /**
     * In the same combat band as the effect's source — i.e. the source creature itself, or a
     * creature sharing the source's band id (CR 702.22). Source-relative: resolves against the
     * source entity supplied in the evaluation context, so it only matches while that source is
     * attacking (band membership exists only during combat). Used for Camel's "this creature and
     * creatures banded with this creature".
     */
    @SerialName("InSameBandAsSource")
    @Serializable
    data object InSameBandAsSource : Entity {
        override val description: String = "in the same band as this creature"
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

    /**
     * Was declared as an attacker at least once during the current turn (set during the
     * declare-attackers step, CR 508.1). Backed by the controller's
     * [com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent] (which
     * the engine already maintains for raid / "attacked this turn" tribal triggers), so it
     * does not need a separate per-entity marker. Survives leaving combat / blockers being
     * declared; cleared at end-of-turn cleanup alongside the player marker.
     */
    @SerialName("AttackedThisTurn")
    @Serializable
    data object AttackedThisTurn : History {
        override val description: String = "attacked this turn"
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

    /**
     * Attached to a permanent whose card matches the given top-level type. Reads the
     * entity's `AttachedToComponent` and checks that the referenced permanent's card
     * has the requested [cardType] (creature, land, artifact, …). Used for "Aura
     * attached to a land" / "Aura attached to a creature" / "Equipment attached to a
     * creature you control" style filters — the attachment is state, not card
     * identity, so it lives here rather than in [CardPredicate].
     *
     * If the entity isn't attached to anything (no `AttachedToComponent`), the
     * predicate is false.
     */
    @SerialName("IsAttachedToCardType")
    @Serializable
    data class AttachedToCardType(val cardType: CardType) : Entity {
        override val description: String = "attached to a ${cardType.displayName.lowercase()}"
    }

    // =============================================================================
    // Saddle (Entity)
    // =============================================================================

    /**
     * Permanent that is currently saddled (CR 702.171b). A marker designation set by a
     * resolved Saddle ability; lasts until end of turn or until the permanent leaves the
     * battlefield. Backed by the engine's `SaddledComponent`. Read by Mount payoffs that
     * gate on "while saddled" / "as long as it's saddled".
     */
    @SerialName("IsSaddled")
    @Serializable
    data object IsSaddled : Entity {
        override val description: String = "saddled"
    }

    // =============================================================================
    // Zone-Specific Markers (Entity)
    // =============================================================================

    /**
     * Card in exile that was put there by the delayed triggered ability of a warp
     * keyword (CR 702.185b). Matches the `WarpExiledComponent` marker the engine
     * writes when a warped permanent leaves the battlefield at end of turn.
     *
     * Useful inside filters that span the exile zone (e.g. an additional cost that
     * lets you choose "a warped creature card you own in exile").
     */
    @SerialName("IsWarpExiled")
    @Serializable
    data object IsWarpExiled : Entity {
        override val description: String = "warped"
    }

    /**
     * Permanent on the battlefield that was cast for its warp cost (CR 702.185).
     * Matches the `WarpedComponent` marker the engine writes when a warped spell
     * resolves — the permanent-side bookkeeping equivalent of 702.185c's "a spell
     * was warped this turn."
     *
     * Useful for effects that branch on whether a target was cast via warp — e.g.,
     * Full Bore's "if that creature was cast for its warp cost, it also gains
     * trample and haste."
     */
    @SerialName("WasCastForWarp")
    @Serializable
    data object WasCastForWarp : Entity {
        override val description: String = "cast for its warp cost"
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
