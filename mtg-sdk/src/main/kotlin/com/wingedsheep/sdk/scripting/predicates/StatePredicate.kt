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

    /**
     * Creature that is blocking the effect's source — i.e. a blocker whose blocked-attacker set
     * contains the source entity supplied in the evaluation context. Source-relative; yields false
     * with no source context or outside combat. Used for "Whenever this becomes blocked, it deals N
     * damage to each creature blocking it" (Battle-Scarred Goblin).
     */
    @SerialName("IsBlockingSource")
    @Serializable
    data object IsBlockingSource : Entity {
        override val description: String = "blocking this creature"
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
     * Dealt combat damage *this turn* to the player who controls the effect's source.
     * Source-relative: resolves `context.sourceId`'s controller and checks whether this
     * creature is recorded as having dealt combat damage to that player this turn. Used for
     * "...a creature that dealt combat damage to you this turn" edicts (Witch-king of Angmar).
     * Backed by a per-turn marker that records, per attacker, which players it connected with;
     * cleared at end-of-turn cleanup. Inert with no source context.
     */
    @SerialName("DealtCombatDamageToSourceControllerThisTurn")
    @Serializable
    data object DealtCombatDamageToSourceControllerThisTurn : History {
        override val description: String = "dealt combat damage to you this turn"
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

    /**
     * This card is currently in a graveyard *and* was put there from the battlefield
     * during the current turn. Used as a target predicate on graveyard-zone filters:
     *
     *  - Samwise the Stouthearted (LTR): "target permanent card in your graveyard
     *    that was put there from the battlefield this turn"
     *  - Lobelia Sackville-Baggins (LTR): same predicate on an opponent's graveyard.
     *
     * Backed by the `PutIntoGraveyardFromBattlefieldThisTurnMarker` data-object
     * component on the card entity. The marker is set by `ZoneTransitionService`
     * whenever a card moves battlefield → graveyard, and stripped when it leaves the
     * graveyard so a later arrival from a different zone (mill, exile → graveyard)
     * does not falsely match. The marker carries no turn number — `BeginningPhaseManager`
     * wipes it from every entity during the untap step of each turn, giving the predicate
     * MTG-correct per-turn semantics independent of the engine's per-round `state.turnNumber`.
     *
     * Pair with `CardPredicate.IsPermanent` (or any other card-predicate constraint)
     * to express the full Samwise / Lobelia filter.
     */
    @SerialName("PutIntoGraveyardFromBattlefieldThisTurn")
    @Serializable
    data object PutIntoGraveyardFromBattlefieldThisTurn : History {
        override val description: String = "put into a graveyard from the battlefield this turn"
    }

    /**
     * This creature blocked, or was blocked by, a legendary creature at some point during the
     * current turn. Used as a target predicate:
     *
     *  - You Cannot Pass! (LTR): "Destroy target creature that blocked or was blocked by a
     *    legendary creature this turn."
     *
     * Backed by the `BlockedOrWasBlockedByLegendaryThisTurnComponent` marker on the creature
     * entity. The marker is stamped at block-declaration time (so the legendary partner's
     * status is captured at pairing time and the predicate keeps matching even if that
     * legendary creature later leaves the battlefield or stops being legendary, per the card's
     * ruling), and cleared at end-of-turn cleanup. Distinct from the combat-only
     * [IsBlocking]/[IsBlocked] predicates, which only hold during the combat phase.
     */
    @SerialName("BlockedOrWasBlockedByLegendaryThisTurn")
    @Serializable
    data object BlockedOrWasBlockedByLegendaryThisTurn : History {
        override val description: String = "that blocked or was blocked by a legendary creature this turn"
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

    /**
     * Has the least power among *all* creatures on the battlefield (global scope, both players),
     * not just the ones its controller controls. On a tie every creature sharing the minimum
     * matches, so a downstream "choose one" selection breaks the tie (Drop of Honey: "destroy the
     * creature with the least power … if two or more are tied, you choose one").
     */
    @SerialName("HasLeastPowerAmongAllCreatures")
    @Serializable
    data object HasLeastPowerAmongAllCreatures : Entity {
        override val description: String = "with the least power"
    }

    /** Has the least power among creatures its controller controls */
    @SerialName("HasLeastPower")
    @Serializable
    data object HasLeastPower : Entity {
        override val description: String = "with the least power"
    }

    /** Is its controller's Ring-bearer (CR 701.54). Used for "you control a Ring-bearer" conditions. */
    @SerialName("IsRingBearer")
    @Serializable
    data object IsRingBearer : Entity {
        override val description: String = "that's a Ring-bearer"
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

    /**
     * Creature that crewed (CR 702.122) or saddled (CR 702.171) the effect's source permanent this
     * turn — i.e. one of the creatures tapped to pay that permanent's Crew/Saddle cost. Source-
     * relative: resolves against the source entity supplied in the evaluation context (its
     * `CrewSaddleContributorsComponent`), so it only matches creatures recorded on *that* Mount /
     * Vehicle. Yields false with no source context. Used for Mount/Vehicle payoffs that target,
     * choose, sacrifice, or return "a creature that crewed/saddled it this turn" (Giant Beaver,
     * Rambling Possum, The Gitrog, Calamity).
     */
    @SerialName("CrewedOrSaddledSourceThisTurn")
    @Serializable
    data object CrewedOrSaddledSourceThisTurn : Entity {
        override val description: String = "that crewed or saddled it this turn"
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
