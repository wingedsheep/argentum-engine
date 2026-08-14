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
    // Zone (Entity)
    // =============================================================================

    /**
     * The object is on the battlefield *right now*.
     *
     * Exists to cancel the last-known-information fallbacks that several Entity predicates carry.
     * [IsAttacking], for one, deliberately falls back to `EntitySnapshot.wasAttacking` once its
     * object has left the battlefield, because that is what a dies trigger asking "was it
     * attacking?" needs (Garna, Bloodfist of Keld). An ability asking whether its *own source* is
     * attacking **now** wants the opposite — an already-dead source must read as not attacking —
     * and gets it by composing `onBattlefield().attacking()`.
     */
    @SerialName("IsOnBattlefield")
    @Serializable
    data object IsOnBattlefield : Entity {
        override val description: String = "on the battlefield"
    }

    // =============================================================================
    // Combat (Entity)
    // =============================================================================

    /**
     * Attacking, and no other creature is attacking (CR 506.5 — "a creature is attacking alone if
     * it's attacking but no other creatures are").
     *
     * Belongs on the *target filter* rather than in an activation restriction: "target creature you
     * control that's attacking alone" is a targeting restriction, so CR 608.2b has to re-check it on
     * resolution and counter the ability when a second attacker showed up in response. An
     * `ActivationRestriction` is only consulted when the ability is activated.
     */
    @SerialName("IsAttackingAlone")
    @Serializable
    data object IsAttackingAlone : Entity {
        override val description: String = "attacking alone"
    }

    @SerialName("IsAttacking")
    @Serializable
    data object IsAttacking : Entity {
        override val description: String = "attacking"
    }

    /**
     * Attacking one of *your* opponents — the player, not their planeswalkers or battles
     * (Oviya, Automech Artisan: "Each creature that's attacking one of your opponents has
     * trample"). "You" is the controller of the ability doing the asking, so this matches
     * regardless of who controls the attacker: a creature an ally controls that's attacking your
     * opponent qualifies, and a creature attacking *you* does not.
     *
     * Strictly narrower than [IsAttacking], which is also true of a creature attacking a
     * planeswalker or battle. Fails closed when there's no controller context to scope "your"
     * against.
     */
    @SerialName("IsAttackingAnOpponent")
    @Serializable
    data object IsAttackingAnOpponent : Entity {
        override val description: String = "attacking one of your opponents"
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

    /**
     * A token that was *created by the effect's source permanent* — its provenance creator id (the
     * `CreatedByComponent` stamped when a `CreateTokenEffect` with `stampCreator = true` made it)
     * equals the source entity supplied in the evaluation context. Source-relative; yields false
     * for non-tokens, tokens with no recorded creator, and with no source context.
     *
     * Backs "tokens created with this creature" provenance (Tetravus: "exile any number of tokens
     * created with this creature"), which "{filter} tokens you control" cannot express when several
     * sources mint the same-named token.
     */
    @SerialName("CreatedBySource")
    @Serializable
    data object CreatedBySource : Entity {
        override val description: String = "created with this creature"
    }

    /**
     * The candidate object (a spell or permanent) is **not** currently the target of an ability on
     * the stack whose source is *another* permanent sharing the effect source's name. Source-relative:
     * reads `context.sourceId`'s name and excludes any stack ability targeting the candidate whose
     * source is a different battlefield permanent with the same name.
     *
     * Backs Goblin Artisans' self-referential targeting restriction: "counter target artifact spell
     * you control **that isn't the target of an ability from another creature named Goblin Artisans**"
     * — so two Goblin Artisans can't both lock onto the same artifact spell. Inert (always true) with
     * no source context.
     *
     * Name comparison reads the base `CardComponent.name`, not the Layer-1 projected name, so a
     * permanent that *copies* the source's name (Clone, Spy Kit) is not recognized as same-named —
     * an accepted edge for these self-referential restrictions.
     */
    @SerialName("NotTargetedByAbilityFromSameNamedSource")
    @Serializable
    data object NotTargetedByAbilityFromSameNamedSource : Entity {
        override val description: String =
            "that isn't the target of an ability from another source with the same name"
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
     * Controlled by a player the effect's *source* dealt combat damage to this turn — the mirror of
     * [DealtCombatDamageToSourceControllerThisTurn]. Source-relative: reads the per-turn recipient
     * marker off `context.sourceId` and asks whether this permanent's controller is among the
     * players it connected with. Used for "…each nonland permanent whose controller was dealt combat
     * damage by this creature this turn" (Steel Hellkite).
     *
     * Controller is evaluated *now*, not at damage time (CR 608.2 — the ability checks the current
     * board): it doesn't matter who controlled the permanent when the damage was dealt, or whether
     * it was even on the battlefield then. Inert with no source context.
     */
    @SerialName("ControllerDealtCombatDamageBySourceThisTurn")
    @Serializable
    data object ControllerDealtCombatDamageBySourceThisTurn : History {
        override val description: String = "whose controller was dealt combat damage by this creature this turn"
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
     * Was declared as an attacker at least once during the current combat (CR 508.1). Backed by the
     * per-entity `AttackedThisCombatComponent` marker, stamped at attacker-declaration time and
     * cleared when the combat phase ends. Resets between multiple combats in one turn, and survives
     * the creature being removed from combat. Pairs with [BlockedThisCombat] for Clockwork Avian's
     * end-of-combat "if this creature attacked or blocked this combat" counter shed.
     */
    @SerialName("AttackedThisCombat")
    @Serializable
    data object AttackedThisCombat : History {
        override val description: String = "attacked this combat"
    }

    /**
     * Was declared as a blocker at least once during the current combat (CR 509.1). Backed by the
     * per-entity `BlockedThisCombatComponent` marker, stamped at blocker-declaration time and cleared
     * when the combat phase ends. Survives the blocked attacker dying (which clears the live
     * `BlockingComponent`), so it still reads true at end of combat.
     */
    @SerialName("BlockedThisCombat")
    @Serializable
    data object BlockedThisCombat : History {
        override val description: String = "blocked this combat"
    }

    /**
     * This card is currently in a graveyard *and* was put there during the current turn,
     * from any zone — the battlefield, but equally the library (mill), the hand (discard),
     * or the stack (a countered or resolved spell). Used as a target predicate on
     * graveyard-zone filters:
     *
     *  - Abyssal Harvester (FDN): "target creature card from a graveyard that was put
     *    there this turn".
     *
     * The zone-restricted sibling is [PutIntoGraveyardFromBattlefieldThisTurn]; both read
     * the same `PutIntoGraveyardThisTurnComponent` (see that predicate's doc for the
     * component's lifecycle), this one ignoring its `fromBattlefield` flag.
     *
     * Pair with `CardPredicate.IsCreature` (or any other card-predicate constraint) to
     * express the full Abyssal Harvester filter.
     */
    @SerialName("PutIntoGraveyardThisTurn")
    @Serializable
    data object PutIntoGraveyardThisTurn : History {
        override val description: String = "put into a graveyard this turn"
    }

    /**
     * This card is currently in a graveyard *and* was put there from the battlefield
     * during the current turn. The zone-restricted sibling of [PutIntoGraveyardThisTurn].
     * Used as a target predicate on graveyard-zone filters:
     *
     *  - Samwise the Stouthearted (LTR): "target permanent card in your graveyard
     *    that was put there from the battlefield this turn"
     *  - Lobelia Sackville-Baggins (LTR): same predicate on an opponent's graveyard.
     *
     * Backed by the `PutIntoGraveyardThisTurnComponent` on the card entity, whose
     * `fromBattlefield` flag this predicate additionally requires. The component is set by
     * `ZoneTransitionService` on every arrival in a graveyard, and stripped when the card
     * leaves the graveyard so a later arrival by a different route does not carry the
     * earlier "from battlefield" claim. It carries no turn number — `BeginningPhaseManager`
     * wipes it from every entity during the untap step of each turn, which is what gives both
     * predicates their per-turn semantics.
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

    /**
     * Is soulbond-**paired** with another creature (CR 702.95b). Negate with [Not] for the
     * "unpaired" adjective the soulbond abilities themselves use ("another unpaired creature you
     * control") — see `GameObjectFilter.paired()` / `.unpaired()`.
     *
     * Deliberately not source-relative: this asks whether the candidate is paired *at all*, so it
     * evaluates the same way in a gather filter, a target filter, and a `Conditions.SourceMatches`
     * gate. "Paired **with the source** specifically" is a different question, answered by
     * [com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair].
     */
    @SerialName("IsPaired")
    @Serializable
    data object IsPaired : Entity {
        override val description: String = "paired"
    }

    // =============================================================================
    // Equipment / Auras (Entity)
    // =============================================================================

    /** Has at least one Equipment attached */
    @SerialName("IsEquipped")
    @Serializable
    data object IsEquipped : Entity {
        override val description: String = "equipped"
    }

    /**
     * Has at least one Aura attached — the MTG adjective "enchanted" (CR 303.4: an Aura *enchants*
     * the permanent it's attached to). The Aura mirror of [IsEquipped], and deliberately narrower
     * than [IsModified]: an Equipment attached or a counter on the permanent does not make it
     * enchanted. Control of the Aura is irrelevant — an opponent's Aura still enchants your
     * creature, which is why "enchanted creatures you control" (A Tale for the Ages) scopes control
     * on the *creature* via a separate controller predicate rather than on the attachment.
     *
     * Role tokens are Auras (CR 113.2c), so this is also the Wilds of Eldraine Roles payoff
     * ("if you control an enchanted creature" — Lord Skitter's Blessing).
     */
    @SerialName("IsEnchanted")
    @Serializable
    data object IsEnchanted : Entity {
        override val description: String = "enchanted"
    }

    /**
     * Has at least one attached Aura whose *controller* satisfies [auraController] — the narrower
     * "enchanted by Auras you control" (Archon of the Wild Rose) as opposed to plain [IsEnchanted],
     * which is agnostic about who controls the Aura (CR 303.4).
     *
     * The two are genuinely different adjectives and both appear in print: A Tale for the Ages
     * buffs your creatures whoever's Aura is on them, while Archon of the Wild Rose only cares
     * about Auras *you* control. Control is read off the Aura at evaluation time, so an Aura
     * changing hands turns the predicate on or off continuously.
     *
     * "You" is the controller of the ability doing the filtering — the source's controller during
     * layer projection, the evaluation context's controller for targets and conditions.
     */
    @SerialName("IsEnchantedByAura")
    @Serializable
    data class IsEnchantedByAura(val auraController: ControllerPredicate) : Entity {
        override val description: String = when (auraController) {
            ControllerPredicate.ControlledByYou -> "enchanted by Auras you control"
            ControllerPredicate.ControlledByOpponent -> "enchanted by Auras an opponent controls"
            else -> "enchanted"
        }
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

    /**
     * Attached to a permanent that matches [filter]. Reads the entity's `AttachedToComponent` and
     * evaluates [filter] against the host using projected battlefield state — so card type, control
     * ("a creature you control"), keywords, P/T, etc. all compose. This is the general form of
     * [AttachedToCardType] (which only checks the host's top-level type): use it whenever the host
     * constraint needs a controller predicate or any card/state predicate, e.g. Stolen Uniform's
     * "if it's attached to a creature you control".
     *
     * The "you" of any controller predicate in [filter] is the controller supplied in the
     * evaluation context (the ability's controller). False if the entity isn't attached to anything.
     */
    @SerialName("IsAttachedTo")
    @Serializable
    data class AttachedTo(val filter: com.wingedsheep.sdk.scripting.GameObjectFilter) : Entity {
        override val description: String = "attached to ${filter.description}"
    }

    // =============================================================================
    // Rooms (Entity)
    // =============================================================================

    /**
     * A Room permanent (CR 709.5) that currently has at least one locked door — i.e. at least
     * one half without its "unlocked" designation (CR 709.5c). Reads the engine's
     * `RoomComponent.lockedFaces`; false for any permanent that isn't a Room or whose doors are
     * all unlocked.
     *
     * Used as a targeting restriction for "unlock a locked door of target Room you control"
     * (Ghostly Keybearer): a fully-unlocked Room has no door left to unlock, so it isn't a legal
     * target.
     */
    @SerialName("HasLockedDoor")
    @Serializable
    data object HasLockedDoor : Entity {
        override val description: String = "with a locked door"
    }

    // =============================================================================
    // Suspect (Entity)
    // =============================================================================

    /**
     * Permanent that is currently suspected (CR 701.60a, Murders at Karlov Manor). A named
     * designation applied by `Effects.Suspect`; unlike saddled it has **no duration** — a
     * suspected permanent stays suspected until it loses the designation or changes zones.
     *
     * Projected, not component-backed: the designation rides a Layer-ability floating effect
     * (`SerializableModification.SetSuspected`) and surfaces as `ProjectedState.isSuspected`,
     * so evaluators must read the projection rather than probing for a component.
     *
     * Menace and "can't block" are separate sub-effects of the same composite, so this predicate
     * asks specifically "is it suspected", not "does it have menace" — which is what cards that
     * read the designation back need ("if it's not suspected", "target suspected creature you
     * control", "if the sacrificed creature was suspected").
     */
    @SerialName("IsSuspected")
    @Serializable
    data object IsSuspected : Entity {
        override val description: String = "suspected"
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

    /**
     * Vehicle/Mount that was crewed (CR 702.122) or saddled (CR 702.171) *by* the effect's source
     * creature this turn — i.e. the source was one of the creatures tapped to pay this permanent's
     * Crew/Saddle cost. The mirror image of [CrewedOrSaddledSourceThisTurn]: there the source is the
     * Vehicle and the candidate is the crewer; here the source is the crewer and the candidate is
     * the Vehicle. Source-relative — resolves against the *candidate's*
     * `CrewSaddleContributorsComponent`, asking whether the source entity is recorded in it. Yields
     * false with no source context. Used for "a Vehicle crewed by this creature this turn" payoffs
     * (Balthier and Fran).
     */
    @SerialName("CrewedOrSaddledBySourceThisTurn")
    @Serializable
    data object CrewedOrSaddledBySourceThisTurn : Entity {
        override val description: String = "crewed or saddled by it this turn"
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

    /**
     * A spell on the stack whose cast-origin zone is [zone] — reads the
     * `SpellOnStackComponent.castFromZone` the engine stamps when the spell is put on the stack
     * (HAND for a normal cast; GRAVEYARD/EXILE/COMMAND for flashback/forage, plot/foretell,
     * commander, …). Composes with [Not] for the common "*wasn't* cast from …" phrasing.
     *
     * Backs Wash Away's base (bracketed) restriction "counter target spell [that wasn't cast from
     * its owner's hand]" as `Not(WasCastFromZone(Zone.HAND))`. A spell can only be cast from its
     * own owner's hand (cards in a hand are owned by that hand's player, CR 108.3), so the
     * owner-scoped wording collapses to the zone check.
     */
    @SerialName("WasCastFromZone")
    @Serializable
    data class WasCastFromZone(val zone: com.wingedsheep.sdk.core.Zone) : Entity {
        override val description: String = "cast from ${zone.displayName}"
    }

    /**
     * The candidate permanent IS the effect's source permanent itself. Source-relative:
     * resolves against the source supplied in the evaluation context, and is false with no
     * source context. This is the [GameObjectFilter][com.wingedsheep.sdk.scripting.GameObjectFilter]
     * counterpart of `GroupFilter`'s `Scope.Self` — use it to scope a filter-carrying static
     * ability to the very permanent that carries it ("this permanent's …" wordings).
     *
     * Backs the granted form of
     * [PreventActivatedAbilities][com.wingedsheep.sdk.scripting.PreventActivatedAbilities]:
     * a permanent granted `PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself())`
     * has *its own* activated abilities locked (Braided Net's "Its activated abilities can't
     * be activated for as long as it remains tapped"), because the activation-legality check
     * evaluates the filter with the grant's holder as the source.
     */
    @SerialName("IsSource")
    @Serializable
    data object IsSource : Entity {
        override val description: String = "this"
    }

    /**
     * The candidate permanent is the *granting permanent* of the ability being resolved — the
     * Equipment/Aura/permanent whose static ability granted the currently-resolving activated or
     * triggered ability (read from the evaluation context's `granterId`). Source-relative to the
     * grant rather than the ability's own source: for a granted triggered ability the source is
     * the equipped creature, but the granter is the Equipment attached to it.
     *
     * Negate with [Not] for "other than [this granting permanent]" exclusions (CR 201.5a) — e.g.
     * Dire Blunderbuss's "sacrifice an artifact other than Dire Blunderbuss". False with no
     * granter context (an ungranted ability).
     */
    @SerialName("IsGrantingPermanent")
    @Serializable
    data object IsGrantingPermanent : Entity {
        override val description: String = "the granting permanent"
    }

    /**
     * The candidate permanent is the permanent the effect's source is attached to — i.e. the
     * creature/permanent enchanted or equipped by the source (read from the source's
     * `AttachedToComponent`). Source-relative: resolves against the source supplied in the
     * evaluation context, and is false if the source isn't attached to anything or there is no
     * source context.
     *
     * Negate with [Not] for "other than enchanted/equipped creature" exclusions in edict-style
     * filters (Sporogenic Infection: "target player sacrifices a creature of their choice other
     * than enchanted creature").
     */
    @SerialName("IsAttachedToBySource")
    @Serializable
    data object IsAttachedToBySource : Entity {
        override val description: String = "that the source is attached to"
    }

    /**
     * The candidate is an Aura/Equipment currently *attached to* the effect's source permanent —
     * the mirror of [IsAttachedToBySource]. Reads the candidate's `AttachedToComponent` and compares
     * its target to the source id. Source-relative; false with no source context or an unattached
     * candidate. Backs "an Equipment attached to it"-style filters where the static ability lives on
     * the *host* (Cloud, Midgar Mercenary), not on the attachment.
     */
    @SerialName("IsAttachedToSource")
    @Serializable
    data object IsAttachedToSource : Entity {
        override val description: String = "attached to this permanent"
    }

    /**
     * The candidate card is one this effect's source permanent exiled — i.e. its entity id is
     * recorded in the source's `LinkedExileComponent` (the same linkage set by
     * `RedirectZoneChange(linkToSource = true)`, `RedirectZoneChangeWithEffect(linkToSource = true)`,
     * `MoveToZoneEffect(linkToSource = true)`, and the `FromLinkedExile` pipeline source).
     * Source-relative: resolves against the source supplied in the evaluation context, and is false
     * if the source has no linked exile or there is no source context.
     *
     * Backs "target creature card exiled with ~" reanimation abilities (The Darkness Crystal), where
     * the ability retrieves a specific card from among those its own permanent banished.
     */
    @SerialName("ExiledWithSource")
    @Serializable
    data object ExiledWithSource : Entity {
        override val description: String = "exiled with this permanent"
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
