package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.scripting.GameObjectFilter
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

    /**
     * The defender-side mirror of [IsAttackingAnOpponent]: attacking *you* or a planeswalker
     * *you* control (Tomik, Wielder of Law: "if two or more of those creatures are attacking you
     * and/or planeswalkers you control"). "You" is the controller of the ability doing the asking,
     * so this matches regardless of who controls the attacker.
     *
     * Deliberately wider than the player-only scoping of
     * [com.wingedsheep.sdk.scripting.EventPattern.CreaturesAttackYouEvent]'s default: that trigger
     * implements CR 509.1b for Orim's Prayer, where an attacker pointed at your planeswalker does
     * *not* count. Cards that print "you and/or planeswalkers you control" want both, and this is
     * the predicate that says so. A creature attacking a *battle* you protect is not included —
     * "planeswalkers you control" is literal.
     *
     * No last-known fallback: like [IsAttackingAnOpponent], the frozen snapshot records only *that*
     * the permanent was attacking, never whom. Fails closed when there's no controller context to
     * scope "you" against.
     */
    @SerialName("IsAttackingYouOrYourPlaneswalkers")
    @Serializable
    data object IsAttackingYouOrYourPlaneswalkers : Entity {
        override val description: String = "attacking you or a planeswalker you control"
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
     * Creature that is blocking the effect's source **or** being blocked by it — the live CR 509
     * pairing in either direction, read off the blocked-attacker sets rather than a snapshot.
     * Source-relative; yields false with no source context or outside combat.
     *
     * The live counterpart of [com.wingedsheep.sdk.scripting.effects.CardSource].LastKnownCombatPairedWithSource,
     * which answers the same question from a leaves-battlefield snapshot for a dies trigger. Use
     * this one while the source is still on the battlefield — "each creature blocking or blocked by
     * this creature" (Spitting Slug).
     */
    @SerialName("IsCombatPairedWithSource")
    @Serializable
    data object IsCombatPairedWithSource : Entity {
        override val description: String = "blocking or blocked by this creature"
    }

    /**
     * Blocking the entity an enclosing `ForEachInGroup` is iterating over — "creatures you control
     * blocking **that creature**" (Tidal Flats), where "that creature" is the loop's current
     * attacker rather than the effect's source. False outside such a loop.
     */
    @SerialName("IsBlockingIterationEntity")
    @Serializable
    data object IsBlockingIterationEntity : Entity {
        override val description: String = "blocking that creature"
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
    // Tap History (History)
    // =============================================================================

    /**
     * This permanent has **become tapped exactly once so far this turn** — the live, state-side
     * reading of "if it's the first time that creature has become tapped this turn" (Captain
     * America, Living Legend).
     *
     * Reads the per-permanent tap counter the `tap()` atom maintains
     * (`HasBecomeTappedComponent`), so it answers *now* rather than repeating what was true when
     * some earlier event happened. That is the whole point of it: the clause it models is a printed
     * intervening "if" (CR 603.4), which is checked when the trigger event occurs **and again as the
     * ability resolves. Only a live read can make the second check mean anything** — a creature
     * untapped and tapped again in response has become tapped twice by resolution, so this predicate
     * turns false and the ability fizzles. The trigger-time half is carried separately by
     * `EventPattern.TapEvent.firstTimeEachTurn`, which reads the tap *event*; use them together, one
     * per check, and see `Conditions.TriggeringPermanentBecameTappedOnlyOnceThisTurn`.
     *
     * "Became tapped" is a transition (CR 701.26a — only untapped permanents can be tapped), so a
     * permanent that entered the battlefield tapped has become tapped zero times and does **not**
     * match; nor does one that has not been tapped at all this turn. Only a count of exactly one
     * matches. The count resets on a zone change (CR 400.7 — a new object) and expires on its own at
     * the turn boundary, with no end-of-turn cleanup to forget.
     */
    @SerialName("BecameTappedOnlyOnceThisTurn")
    @Serializable
    data object BecameTappedOnlyOnceThisTurn : History {
        override val description: String = "became tapped for the first time this turn"
    }

    // =============================================================================
    // Counter History (History)
    // =============================================================================

    /**
     * One or more counters were put on this permanent during the current turn — the filter-level
     * form of "…that you've put one or more +1/+1 counters on this turn".
     *
     * Backed by the per-permanent `ReceivedCountersThisTurnComponent`, which the counter-placement
     * paths stamp and end-of-turn cleanup clears. The facts are recorded **at placement time**, so
     * the predicate keeps matching after the counters themselves have been removed — which is what
     * the printed wording asks ("what you *put on* it", not "what is on it now"). Compose with
     * [HasCounter] when a card really does want counters still present.
     *
     * Both parameters default to the widest reading and narrow it along the two axes printed cards
     * vary:
     *  - [counterType] (e.g. `Counters.PLUS_ONE_PLUS_ONE`) restricts it to one kind of counter, so a
     *    stun or shield counter doesn't satisfy a "+1/+1 counters" clause.
     *  - [placedByController] restricts it to counters put on by the permanent's own controller —
     *    the "**you've** put" half. Named for the controller rather than "you" because that is what
     *    the marker records (the placer is compared to the permanent's projected controller at
     *    placement time); on the "creature **you control**" filters these clauses always carry, the
     *    two readings coincide. An opponent proliferating your creature does not satisfy it.
     *
     * Used by Kid Loki ("Each creature you control that you've put one or more +1/+1 counters on
     * this turn has hexproof") at group-static scope, and — via
     * `Conditions.SourceReceivedCounterThisTurn`, which is `SourceMatches` over this predicate — by
     * Beast, Erudite Aerialist and Fractal Tender at source scope.
     */
    @SerialName("ReceivedCounterThisTurn")
    @Serializable
    data class ReceivedCounterThisTurn(
        val counterType: String? = null,
        val placedByController: Boolean = false
    ) : History {
        // Rendered in the adjective slot a GameObjectFilter puts state predicates in, ahead of the
        // type word — so it reads as a bare qualifying phrase, like "was dealt damage this turn".
        // Kept as a *verb* phrase ("had … put on it …") rather than a subject-led one ("you've put
        // …") so it also reads correctly through `EntityMatches(Self, …)`, which renders a filter
        // as "if this ${filter.description}" — that is the source-scoped view this predicate backs
        // via `Conditions.SourceReceivedCounterThisTurn`.
        override val description: String = buildString {
            append("had ")
            append(counterType?.let { "one or more $it counters" } ?: "one or more counters")
            append(" put on it ")
            if (placedByController) append("by you ")
            append("this turn")
        }
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

    /**
     * This permanent has *dealt* damage — the active voice, mirroring [WasDealtDamageThisTurn]'s
     * passive one. [thisTurnOnly] picks the window:
     *
     *  - `false` (default, the widest reading): ever, since it entered the battlefield — "as long as
     *    this creature hasn't dealt damage" (Karakyk Guardian), via `Conditions.SourceHasDealtDamage`.
     *  - `true`: during the current turn only — "target creature an opponent controls that dealt
     *    damage this turn" (Red Guardian, Super-Soldier).
     *
     * One fact, two windows, so both read the same per-permanent marker rather than a parallel
     * tracker: the engine's `HasDealtDamageComponent` records the turn number of the permanent's most
     * recent damage, which every damage-dealing path stamps. Presence answers the lifetime window;
     * comparing the stamp against the current turn answers the per-turn one. Nothing has to be
     * cleared at end of turn — a stale stamp simply stops matching once the turn number moves on —
     * and no damage path can record one window without recording the other.
     *
     * Both windows reset when the permanent changes zones (CR 400.7 — it comes back a new object with
     * no memory), which is what "that dealt damage this turn" asks for: the object in front of you
     * must be the one that dealt it.
     *
     * Damage *type* is not an axis here — combat and noncombat damage both count, matching the
     * printed wording. "Dealt combat damage" specifically has its own predicates
     * ([HasDealtCombatDamageToPlayer], [DealtCombatDamageToSourceControllerThisTurn]) because those
     * also scope by recipient.
     */
    @SerialName("HasDealtDamage")
    @Serializable
    data class HasDealtDamage(val thisTurnOnly: Boolean = false) : History {
        override val description: String =
            if (thisTurnOnly) "dealt damage this turn" else "has dealt damage"
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
     * The creature **couldn't have been declared as an attacker** this turn — the "except for
     * creatures that couldn't attack" clause of Season of the Witch, which spares a creature that
     * had no choice in the matter rather than punishing it for staying home.
     *
     * The reasons a creature had no choice. Who may be declared, and by whom, is CR 508.1a:
     *
     *  - **its controller wasn't attacking this turn.** Only the active player declares attackers,
     *    so every creature an opponent controls is spared — the sweep is one-sided in practice,
     *    hitting only the creatures that skipped the turn's Declare Attackers Step. In a
     *    shared-team-turns format the whole active team counts (CR 805.10b).
     *  - **no Declare Attackers Step happened at all.** The clause above asks whose turn it is,
     *    which is not the same question: an effect that skips the combat phase (False Peace,
     *    Fatespinner) means nobody was ever offered the choice, so nobody stayed home by choice.
     *  - **summoning sickness** — it entered this turn without haste.
     *
     * And the attack *restrictions*, CR 508.1c: **defender** (CR 702.3b) or a **"can't attack"**
     * effect (Pacifism). All of these read from projected state where they can, so granted/removed
     * keywords and effects count.
     *
     * It deliberately does *not* re-run the full declare-attackers legality check — that needs a
     * chosen defending player and a `CardRestrictionsError`-style card registry, neither of which
     * exists in predicate evaluation — so a creature kept home only by a card-specific "can't
     * attack unless …" restriction is not spared, and neither is one that came under its
     * controller's control this turn without entering the battlefield. Pair with [AttackedThisTurn]
     * negated to get "didn't attack and could have".
     */
    @SerialName("CouldNotHaveAttackedThisTurn")
    @Serializable
    data object CouldNotHaveAttackedThisTurn : History {
        override val description: String = "couldn't attack"
    }

    /**
     * Was declared as an attacker during its controller's **most recent own turn** — "it attacked
     * during your last turn". Backed by `PlayerAttackersLastTurnComponent`, which the cleanup step
     * rolls over from the this-turn set on that player's own turn only, so an intervening
     * opponent's turn can't blank it.
     *
     * Distinct from [AttackedThisTurn] in both direction and lifetime: this one is false on the
     * turn the creature actually attacked and true on the next one. It is what gates the untap step
     * for Goblin Rock Sled and Tangle Kelp ("doesn't untap during your untap step if it attacked
     * during your last turn") — the untap step runs before that turn's cleanup, so the record it
     * reads is genuinely the previous turn's.
     */
    @SerialName("AttackedLastTurn")
    @Serializable
    data object AttackedLastTurn : History {
        override val description: String = "attacked during its controller's last turn"
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
     * Was declared as a blocker at least once **this turn** (CR 509.1). Backed by the per-entity
     * `BlockedThisTurnComponent`, stamped at blocker declaration and cleared at end of turn — so,
     * unlike [BlockedThisCombat], it survives into the postcombat main phase and across a second
     * combat in the same turn.
     *
     * Pairs with [AttackedThisTurn] for "unless it attacked or blocked this turn" (Lurker).
     */
    @SerialName("BlockedThisTurn")
    @Serializable
    data object BlockedThisTurn : History {
        override val description: String = "blocked this turn"
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

    /**
     * Has a **disguise** ability (CR 702.168) — the printed keyword, read off the card definition,
     * so it answers the same in every zone.
     *
     * Deliberately *not* folded into [HasMorphAbility], which asks "can this be turned face up by a
     * morph-family procedure" and therefore also answers true for a permanent that is face down for
     * some other reason. Disguise is a printed ability of a card, and Expose the Culprit's "any
     * number of face-up creatures you control **with disguise**" asks about the card's abilities
     * while it is face **up** — a moment at which no turn-up procedure exists to inspect. A cloaked
     * permanent is face down without having disguise; a card with disguise sitting in hand has it
     * without being face down. This predicate is the printed-ability half only.
     */
    @SerialName("HasDisguiseAbility")
    @Serializable
    data object HasDisguiseAbility : Entity {
        override val description: String = "with disguise"
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

    /**
     * Has the least mana value among battlefield permanents matching [candidates]. Ties match every
     * permanent sharing the minimum, allowing ordinary target selection to choose among them.
     */
    @SerialName("HasLeastManaValueAmong")
    @Serializable
    data class HasLeastManaValueAmong(
        val candidates: GameObjectFilter
    ) : Entity {
        override val description: String = "with the least mana value among ${candidates.description}"
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

    /**
     * The candidate's *controller* controls at least one permanent matching [filter] — "target
     * creature whose controller controls an Island" (Seasinger). The nested filter is evaluated
     * against projected battlefield state, and its "you" is bound to the candidate's controller
     * rather than to the ability's controller, which is the whole point: the constraint is about
     * the creature's owner-of-the-moment, not about who is casting.
     */
    @SerialName("ControllerControls")
    @Serializable
    data class ControllerControls(
        val filter: com.wingedsheep.sdk.scripting.GameObjectFilter
    ) : Entity {
        override val description: String = "whose controller controls ${filter.indefiniteArticle} ${filter.description}"
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
    // Solved (Entity)
    // =============================================================================

    /**
     * Permanent that currently has the solved designation (CR 719.3b, Murders at Karlov Manor).
     * Set by `Effects.BecomeSolved` when a Case's "To solve" trigger resolves.
     *
     * Sticky and one-way: once a permanent becomes solved it stays solved until it leaves the
     * battlefield, and there is no "unsolve". That is why this is component-backed (the engine's
     * `SolvedComponent`) like [IsSaddled] rather than a floating layer effect like [IsSuspected] —
     * the designation is neither an ability nor part of the permanent's copiable values, so a copy
     * of a solved Case is not itself solved.
     *
     * Read by the "Solved —" abilities (CR 702.169) through `Conditions.SourceIsSolved`, which gates
     * them as a static condition, an intervening-if, or an activation restriction depending on the
     * ability's kind.
     */
    @SerialName("IsSolved")
    @Serializable
    data object IsSolved : Entity {
        override val description: String = "solved"
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
