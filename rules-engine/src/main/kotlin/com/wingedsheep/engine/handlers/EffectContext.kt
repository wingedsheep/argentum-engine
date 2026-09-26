package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.battlefield.chosenOpponent

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Context for effect execution.
 *
 * Core fields (sourceId, controllerId, targets, etc.) are always relevant.
 * Pipeline-specific state (collections, named values, iteration targets) lives
 * in [pipeline] to keep the two concerns separate and make pipeline extensions
 * self-contained.
 */
@Serializable
data class EffectContext(
    // --- Core ---
    val sourceId: EntityId?,
    val controllerId: EntityId,
    /**
     * The controller of the *overall* effect/ability, stable across per-player iteration.
     * `ForEachEffect(IterationSpace.Players)` rebinds [controllerId] to each iterated player so
     * `Player.You` resolves to them, but some sub-effects still need the original activating
     * player — e.g. a "until **your** next turn" window whose "your" is the activating player for
     * every affected player (Memory Vessel). Captured on the first Players-iteration and preserved
     * across nested iterations. Null outside iteration; read as `effectControllerId ?: controllerId`.
     */
    val effectControllerId: EntityId? = null,
    /**
     * The permanent whose static ability granted the currently-resolving ability (the
     * Equipment/Aura/permanent bearing the `GrantActivatedAbility` static), captured when the
     * ability was put on the stack. Resolves [com.wingedsheep.sdk.scripting.targets.EffectTarget.GrantingSource]
     * so a granted ability can name its granter — e.g. an Equipment that gives its bearer
     * "...Return [this Equipment] to its owner's hand" (Trusty Boomerang). Null for non-granted
     * abilities and for spell resolution; the granter may have left play by resolution, so the
     * referencing effect must tolerate a now-absent entity (CR 113.7a).
     */
    val granterId: EntityId? = null,
    /**
     * Definition-scoped identity of the triggered/activated ability currently resolving, copied
     * from its stack component (see [com.wingedsheep.sdk.scripting.AbilityIdentity]). Lets a
     * resolution-time may-question consult the controller's persistent auto-answer yields without
     * re-deriving the key. Null for spell resolution, sources with no card definition, and
     * activated abilities whose lookup did not prove definition ownership (backlog §C).
     */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
    /**
     * The concrete ability captured at activation. Resolution must not rediscover it from a
     * source or grant that can change before resolving (including "retain this ability" copies).
     * Null for spells, triggers, and synthesized activations without an ActivatedAbility.
     */
    val activatedAbility: com.wingedsheep.sdk.scripting.ActivatedAbility? = null,
    /**
     * The player currently under consideration as a target, bound while evaluating a
     * `TargetPlayer.restriction` / `TargetOpponent.restriction` (CR 115). Resolves
     * [com.wingedsheep.sdk.scripting.references.Player.Candidate]. Null in every normal
     * effect-resolution context — there is no candidate once an effect is executing.
     */
    val candidatePlayerId: EntityId? = null,
    /**
     * The face-change tally [sourceId] carried when this ability was put onto the stack (CR
     * 701.28f). `TransformEffectExecutor` compares it against the source's current tally and
     * ignores a self-transform when the two differ — the permanent has already turned over since,
     * so the instruction does nothing. Null for spells, for a non-double-faced source, and for
     * synthesized abilities that carry no such restriction.
     */
    val sourceFaceChanges: Int? = null,
    /** Battlefield visit of the resolving ability's source. */
    val sourceBattlefieldTimestamp: Long? = null,
    /**
     * The source had already returned as a different permanent when this ability began resolving.
     * Retain sourceId for linked data and last-known information, but do not act on that new permanent.
     * Frozen at resolution start so instructions can still track a source they themselves return.
     */
    val sourceReferenceLost: Boolean = false,
    val triggeringReferenceLost: Boolean = false,
    /** The loop's current object ([iterationEntityId]) has changed zones since the loop bound it. */
    val iterationReferenceLost: Boolean = false,
    val objectReferences: ObjectReferenceEnvironment = ObjectReferenceEnvironment(),
    val targets: List<ChosenTarget> = emptyList(),
    /**
     * Positionally-aligned view of [targets]: the same length as the originally-chosen target
     * list, with `null` in any slot whose target was dropped by resolution-time legality
     * validation (CR 608.2b). Populated on the spell-resolution path (and copied through
     * composite/iteration sub-effects); empty elsewhere, where it coincides with [targets].
     *
     * Positional target references — [EffectTarget.ContextTarget],
     * [com.wingedsheep.sdk.scripting.references.Player.ContextPlayer], and indexed conditions —
     * MUST resolve through [positionalTarget] so a now-illegal slot reads `null` (and the
     * sub-effect fizzles, CR 608.2b) instead of silently consuming the next still-legal target
     * whose position shifted forward in the compacted [targets] list. Diplomatic Relations is
     * the canonical case: "creature you control" dies in response, and without this the damage
     * amount's `ContextTarget(0)` power read would land on the surviving opponent's creature.
     */
    val alignedTargets: List<ChosenTarget?> = emptyList(),
    /**
     * The X chosen for an X-cost spell/ability. Also reused by `ChooseNumberThenEffect` to
     * carry a "choose a number" value into the inner effect (read via `CardPredicate.ManaValueEqualsX`,
     * Void). These two uses share one slot, so a future card that both pays `{X}` *and* chooses a
     * number would collide here — split the slot before authoring such a card.
     */
    val xValue: Int? = null,
    /**
     * Total mana paid from the pool to cast this spell — sum of every `manaSpent{Color}`
     * bucket on the [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent].
     * For `{X}` spells the X portion is already included in those buckets, so this is
     * not the same as [xValue]. Used by `DynamicAmount.TotalManaSpent`.
     */
    val totalManaSpent: Int = 0,
    /**
     * Per-color mana spent on the `{X}` portion of the spell or activated ability, for a
     * color-restricted X (e.g. Soul Burn's "spend only black and/or red mana on X").
     * Read by `DynamicAmount.ManaSpentOnX`. Empty when X was unrestricted.
     */
    val manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
    /**
     * The optional-additional-cost mechanic declared for the spell being cast or resolved
     * ([com.wingedsheep.sdk.scripting.ChoiceSlot.KICKED] for kicker, `BARGAINED` for bargain), or
     * null when none was. Read by `WasKicked` and by `CastChoiceMade(slot)` — the latter is how a
     * spell's own "if this spell was bargained" rider resolves while the spell is still on the
     * stack, before any durable cast-choices bag exists, and how a `CostGating.OnlyIf` cost
     * reduction is priced against the branch being enumerated.
     */
    val declaredCostSlot: ChoiceSlot? = null,
    /** True if the spell's optional Blight additional cost was paid (BlightOrPay path chosen). */
    val wasBlightPaid: Boolean = false,
    /**
     * True if the spell's waterbend additional cost was paid (Avatar: The Last Airbender) —
     * mandatory always, optional "you may waterbend {N}" only when elected. Read by
     * `WaterbendWasPaid`.
     */
    val wasWaterbendPaid: Boolean = false,
    /** True if the spell was cast for its sneak cost (CR 702.190). Read by `SneakCostWasPaid`. */
    val wasSneaked: Boolean = false,
    /** True if the spell was cast using web-slinging (CR 702.188). Read by `WebSlungCostWasPaid`. */
    val wasWebSlung: Boolean = false,
    /** True if the spell was cast for its mayhem cost (CR 702.187). Read by `MayhemCostWasPaid`. */
    val wasMayhem: Boolean = false,
    // --- Cast-time state ---
    /**
     * Projected snapshots of permanents sacrificed as part of the cost (Rule 113.7a /
     * 608.2h — "as it last existed on the battlefield"). Captured before the zone change
     * so downstream effects can read power, toughness, and subtypes after the permanent
     * has left the battlefield.
     */
    val sacrificedPermanents: List<EntitySnapshot> = emptyList(),
    /**
     * Entity ids of cards discarded to pay this spell's additional discard cost
     * (`Costs.additional.DiscardCards(...)`) or this activated ability's discard cost. By
     * resolution these cards live in their owner's graveyard (CR 608.2), so
     * [EffectTarget.DiscardedAsCost] resolves to the id and an `EntityMatches` reads the card's
     * graveyard characteristics (Grab the Prize, Hisoka, Minamo Sensei). Empty when the spell or
     * ability carried no discard cost.
     */
    val discardedAsCostCards: List<EntityId> = emptyList(),
    /** Division announced as the spell/ability went on the stack (CR 601.2d), target ID -> share: damage for
     *  a DividedDamageEffect, counters for a triggered DistributeCountersAmongTargetsEffect. */
    val damageDistribution: Map<EntityId, Int>? = null,
    /**
     * Pre-chosen modes for modal spells/abilities (700.2). Populated at stack resolution
     * from either [SpellOnStackComponent] or [TriggeredAbilityOnStackComponent]. When
     * non-empty, [ModalEffectExecutor] iterates these modes with per-mode targets from
     * [modeTargetsOrdered] instead of prompting for a mode decision.
     */
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
    /** Number of cards exiled as an additional cost (for ExileVariableCards) */
    val exiledCardCount: Int = 0,
    /** X chosen for [com.wingedsheep.sdk.scripting.AdditionalCost.BlightVariable] */
    val additionalCostBlightAmount: Int = 0,
    /** Permanents tapped as part of an activated ability's cost (e.g., Cryptic Gateway) */
    val tappedPermanents: List<EntityId> = emptyList(),
    /**
     * Cards exiled to pay an activated ability's cost, recorded at payment time (CR 601.2h — the
     * cost is paid on activation, long before the ability resolves). Read by
     * [com.wingedsheep.sdk.scripting.effects.CardSource.ExiledAsCost] so the resolving effect can
     * name "those exiled cards" (Baron Helmut Zemo). The exile counterpart of [tappedPermanents],
     * and scoped to *this* activation's payment — unlike a permanent's linked-exile pile, which
     * accumulates across activations.
     */
    val exiledAsCostCards: List<EntityId> = emptyList(),
    /**
     * LKI snapshots (Rule 113.7a) for the entries of [exiledAsCostCards] that were exiled **from
     * the battlefield**, captured before the zone change. A permanent exiled as a cost may be a
     * token — which ceases to exist and can't be read at resolution — or may have been a Thrull
     * only through a continuous effect, so "the exiled creature was a Thrull" (Soul Exchange) has
     * to read what it last was on the battlefield rather than what its card prints. Empty for
     * exile costs paid from any other zone, where the card is still a real object in exile and its
     * printed characteristics are the right answer.
     */
    val exiledAsCostSnapshots: List<EntitySnapshot> = emptyList(),
    /** LKI snapshots for [tappedPermanents] (Rule 113.7a). See [EntitySnapshot]. */
    val tappedEntitySnapshots: List<EntitySnapshot> = emptyList(),
    /**
     * Counters (kind → count) the source had the moment a self-exile /
     * self-sacrifice cost wiped them (CR 113.7a). Read by
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.LastKnownSourceCounters] so an effect
     * like "Draw a card for each verse counter on this. If it had seven or more..." (Lost Isle
     * Calling) sees the pre-cost count rather than zero.
     */
    val lastKnownSourceCounters: Map<CounterType, Int> = emptyMap(),
    /**
     * Frozen projected P/T (and subtypes/supertypes) the source had the moment a self-exile /
     * self-sacrifice cost moved it off the battlefield (CR 113.7a / 608.2h — "as it last existed
     * on the battlefield"). Mirrors [lastKnownSourceCounters]. Read by [DynamicAmountEvaluator]
     * when an `EntityProperty(EffectTarget.Self, …)` power/toughness read resolves after the
     * source is gone, so "Sacrifice this creature: it deals damage equal to its power" reads the
     * pre-sacrifice power rather than zero (Blazing Bomb's Blow Up, Cinder Shade, Ghitu Fire-Eater).
     * Null when the cost did not sacrifice/exile the source.
     */
    val lastKnownSourceSnapshot: EntitySnapshot? = null,
    /**
     * Entity ids of the permanents (Equipment/Auras) that were attached to the source the moment a
     * self-sacrifice / self-exile cost moved it off the battlefield (CR 113.7a). Captured before the
     * cost is paid, while the source still carries its `AttachmentsComponent`. Read by
     * [com.wingedsheep.sdk.scripting.effects.CardSource.LastKnownEquipmentAttachedToSource] so an
     * effect can "attach an Equipment that was attached to it to that creature" (Zack Fair) after the
     * source — and its live attachment index — are gone. Empty when the cost did not sacrifice/exile
     * the source or it had no attachments.
     */
    val lastKnownSourceAttachments: List<EntityId> = emptyList(),
    /**
     * LKI snapshots (Rule 113.7a) for entities chosen via an additional cost
     * step like [com.wingedsheep.sdk.scripting.AdditionalCost.ChooseEntity]
     * with `captureSnapshot = true`. Indexed by entity id via
     * [com.wingedsheep.engine.state.components.stack.snapshotFor]. Read by
     * [DynamicAmountEvaluator] when the `EntityProperty` path resolves an
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget].
     */
    val chosenEntitySnapshots: List<EntitySnapshot> = emptyList(),
    // --- Trigger state ---
    /**
     * Everything the trigger event said about why the resolving ability fired — damage amount,
     * counter counts, last-known power / toughness / types / counters, the scry count, the clash
     * outcome, the triggering spell's mana spent, the host an attachment came off, the spell that
     * targeted a warded permanent, … — as one record copied from the stack object
     * ([TriggeredAbilityOnStackComponent.triggerContext]) or the pending trigger. Readers go
     * through it (`context.triggerContext?.scryCount`) rather than a field per fact, so a new
     * trigger fact is one field on [com.wingedsheep.engine.event.TriggerContext], its producer, and
     * its reader. Null for spell resolution and every other non-triggered context.
     *
     * [triggeringEntityId], [triggeringPlayerId] and [xValue] stay separate slots: iteration,
     * spell resolution, delayed triggers and cast-time copies write them without any trigger
     * record, so they are rebindable context rather than trigger facts. The record's own copies
     * of those three are the as-fired values and are only read to rebuild a reflexive trigger.
     */
    val triggerContext: com.wingedsheep.engine.event.TriggerContext? = null,
    /** The entity that caused the trigger to fire (e.g., creature that dealt damage for Aurification) */
    val triggeringEntityId: EntityId? = null,
    /** The player associated with the trigger event (e.g., the player who cast a spell for SpellCastEvent) */
    val triggeringPlayerId: EntityId? = null,
    /**
     * The defending player for a per-defender combat legality check (CR 508.1 attack
     * declaration). Bound by [com.wingedsheep.engine.mechanics.combat.rules.CantAttackUnlessDefenderRule]
     * so `Player.DefendingPlayer` conditions ("can't attack unless defending player controls
     * an Island") evaluate against the player actually being attacked — the source has no
     * `AttackingComponent` yet at declaration time, so the attack-time resolution path can't
     * supply it.
     */
    val defendingPlayerId: EntityId? = null,
    // --- Choice state ---
    /** Color chosen for "add one mana of any color" abilities */
    val manaColorChoice: Color? = null,
    /**
     * Color chosen during a [com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect]
     * resolution. Set by the resumer before dispatching the inner effect; read by atomic
     * "...FromChosenColor" executors.
     */
    val chosenColor: Color? = null,
    /**
     * Every color picked by a multi-color [com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect]
     * ("the color or colors of your choice" — Quickchange). Empty for a single-color choice, where
     * [chosenColor] is the whole answer; when non-empty it contains [chosenColor].
     */
    val chosenColors: Set<Color> = emptySet(),
    /** Creature type chosen during casting (e.g., Aphetto Dredging) */
    val chosenCreatureType: String? = null,
    /**
     * The opponent the controller picked to make a
     * [com.wingedsheep.sdk.scripting.effects.Chooser.Opponent] decision, when the game has more
     * than one opponent to choose from (CR 601.7a / 602.3a and the matching resolution-time
     * rulings). Set by
     * [com.wingedsheep.engine.core.ChooseOpponentDeciderContinuation]'s resumer just before the
     * effect is re-executed, and read back by
     * [com.wingedsheep.engine.handlers.effects.ChooserResolution.resolve].
     *
     * Resolution-scoped on purpose: the stamp rides only the re-run context, so each separate
     * "an opponent chooses" step gets its own prompt. Null in two-player games (a sole opponent
     * is a forced choice, never asked) and before the pick is made.
     */
    val opponentDeciderId: EntityId? = null,
    // --- Zone state ---
    /** Zone the spell was cast from (e.g., HAND, GRAVEYARD for flashback) */
    val castFromZone: Zone? = null,
    // --- Projection state ---
    /** The entity being modified during continuous effect projection (for DynamicAmount evaluation) */
    val affectedEntityId: EntityId? = null,
    // --- Pipeline state ---
    val pipeline: PipelineState = PipelineState.EMPTY,
    // --- Safety ---
    /**
     * How many effect-executions deep this context is within a single resolution. Bumped by one
     * each time [com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry] recurses into a
     * sub-effect (composite / iteration / draw / chain executors), and per iteration by
     * `RepeatWhileEffect`. The registry aborts the branch once this exceeds
     * [com.wingedsheep.engine.core.GameLimits.MAX_RESOLUTION_DEPTH], so an unbounded effect loop
     * fails closed instead of `StackOverflowError`. Lives on the (immutable) context rather than
     * on the shared registry so it stays correct under the AI's parallel state evaluation.
     */
    val resolutionDepth: Int = 0
) {
    val activatedAbilityId: com.wingedsheep.sdk.scripting.AbilityId?
        get() = activatedAbility?.id

    /**
     * The object an enclosing `ForEach` loop over a group or a collection is visiting —
     * [EffectTarget.IterationEntity]. Null outside such a loop.
     */
    val iterationEntityId: EntityId?
        get() = objectReferences.iteration?.entityId

    /**
     * Resolve a symbolic effect target to a concrete entity id using just the context.
     *
     * Stateless resolution — handles self, controller, context targets, bound variables,
     * pipeline targets, triggering entity, etc. For relational references that need to look
     * up components (e.g., [EffectTarget.EnchantedCreature], [EffectTarget.TargetController]),
     * use the overload that also takes [GameState].
     */
    /**
     * Resolve a chosen target by its ORIGINAL declared position, stable even when CR 608.2b
     * validation dropped an earlier target before resolution. Every positional
     * `targets[index]` read that uses a card-definition-supplied index goes through here so a
     * dropped target doesn't shift later targets forward in the compacted [targets] list.
     *
     * [alignedTargets] is only consulted when it is genuinely the position-preserving partner
     * of the CURRENT [targets] — i.e. dropping its `null` slots reproduces [targets] exactly.
     * That guard matters because executors re-scope [targets] to a different list (e.g.
     * [com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect] iterates one target at a time,
     * continuation resumers narrow to the remaining/selected targets) via `copy()`, which
     * carries the now-stale parent [alignedTargets]. In that case we fall back to [targets] so
     * `ContextTarget(0)` reads the re-scoped target, not the stale slot 0. When the lists do
     * coincide (top-level spell resolution), [alignedTargets] supplies the `null` for any
     * dropped slot.
     */
    fun positionalTarget(index: Int): ChosenTarget? =
        if (alignedTargets.isNotEmpty() && alignedTargets.filterNotNull() == targets) {
            alignedTargets.getOrNull(index)
        } else {
            targets.getOrNull(index)
        }

    /** Capture legacy battlefield validity and bind this resolution's independent identity scope. */
    fun forAbilityResolution(state: GameState, resolutionId: EntityId? = null): EffectContext {
        val currentVisit = sourceId?.let { state.getEntity(it) }
            ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()?.timestamp
        // Older serialized abilities have no provable historical identity. Keep their LKI and
        // remaining instructions, but fail closed for actionable source/trigger references.
        val captured = objectReferences.copy(captured = true)
        return copy(
            objectReferences = captured.copy(resolutionKey = resolutionId?.let { id ->
                "$id:${state.objectRef(id)?.generation}"
            } ?: captured.resolutionKey),
            sourceReferenceLost = sourceBattlefieldTimestamp != null && currentVisit != null &&
                currentVisit != sourceBattlefieldTimestamp,
        ).withCurrentObjectReferences(state)
    }

    /** Recheck on every instruction/resume; an unrelated move while paused cannot be followed. */
    fun withCurrentObjectReferences(state: GameState): EffectContext = copy(
        sourceReferenceLost = if (objectReferences.captured) {
            !objectReferences.isCurrent(objectReferences.source, state)
        } else sourceReferenceLost,
        triggeringReferenceLost = triggeringEntityId !in state.turnOrder &&
            !objectReferences.isCurrent(objectReferences.triggering, state),
        iterationReferenceLost = objectReferences.iteration != null &&
            !objectReferences.isIterationCurrent(state),
    )

    fun authorizeObjectMoves(events: List<com.wingedsheep.engine.core.GameEvent>): EffectContext =
        copy(objectReferences = objectReferences.authorize(events))

    fun chosenOpponent(state: GameState): EntityId? =
        pipeline.storedCollections[RESOLUTION_CHOSEN_OPPONENT]?.firstOrNull()
            ?: sourceId?.takeIf { objectReferences.isCurrent(objectReferences.source, state) }
                ?.let { state.getEntity(it)?.chosenOpponent() }


    /**
     * Battlefield-only instructions cannot affect a source — or a loop's current object — that has
     * left the battlefield or already returned as a new object.
     */
    fun isUnavailableBattlefieldSource(target: EffectTarget, state: GameState): Boolean = when (target) {
        EffectTarget.Self -> sourceReferenceLost ||
            (sourceBattlefieldTimestamp != null && sourceId !in state.getBattlefield())
        EffectTarget.IterationEntity -> objectReferences.iteration != null &&
            (iterationReferenceLost || iterationEntityId !in state.getBattlefield())
        else -> false
    }

    fun resolveTarget(target: EffectTarget): EntityId? =
        TargetResolutionUtils.resolveTarget(target, this)

    /**
     * Resolve a symbolic effect target to a concrete entity id, consulting [state] for
     * relational references (attachments, controllers, owners).
     */
    fun resolveTarget(target: EffectTarget, state: GameState): EntityId? =
        TargetResolutionUtils.resolveTarget(target, this, state)

    /**
     * Resolve a symbolic effect target and throw if it cannot be resolved.
     * Use when the caller knows by construction that the target must exist.
     */
    fun requireTarget(target: EffectTarget): EntityId =
        resolveTarget(target) ?: error("Cannot resolve target: $target")

    /**
     * Resolve a symbolic effect target using [state] and throw if it cannot be resolved.
     */
    fun requireTarget(target: EffectTarget, state: GameState): EntityId =
        resolveTarget(target, state) ?: error("Cannot resolve target: $target")

    /**
     * Resolve a player reference target (e.g., "target player", "each opponent") to a
     * single player entity id. Stateless overload — see [resolvePlayerTargets] for
     * multi-player results.
     */
    fun resolvePlayerTarget(target: EffectTarget): EntityId? =
        TargetResolutionUtils.resolvePlayerTarget(target, this)

    /**
     * Resolve a player reference target, consulting [state] for relational references
     * like [com.wingedsheep.sdk.scripting.references.Player.OwnerOf] /
     * [com.wingedsheep.sdk.scripting.references.Player.ControllerOf].
     */
    fun resolvePlayerTarget(target: EffectTarget, state: GameState): EntityId? =
        TargetResolutionUtils.resolvePlayerTarget(target, this, state)

    /**
     * Resolve a player reference target to a list of player ids (for multi-player effects
     * like "each player" / "each opponent").
     */
    fun resolvePlayerTargets(target: EffectTarget, state: GameState): List<EntityId> =
        TargetResolutionUtils.resolvePlayerTargets(target, state, this)

    companion object {
        /**
         * Build a named targets map from target requirements and chosen targets.
         *
         * For each requirement with a non-null `id`:
         * - If count == 1: maps `id` -> chosenTarget
         * - If count > 1: maps `id[0]` -> target0, `id[1]` -> target1, etc.
         *
         * Requirements with `id == null` are skipped (backward compat with ContextTarget).
         *
         * `targets` is accepted as `List<ChosenTarget?>` so callers can pass a
         * **positionally-aligned** list where slots whose target was dropped by
         * resolution-time legality validation (CR 608.2b) are `null`. Such slots
         * leave the corresponding `id` unmapped so any sub-effect that references
         * the now-illegal target via [EffectTarget.BoundVariable] resolves to
         * `null` and fizzles instead of silently consuming a later target whose
         * position shifted forward in the compacted list. The non-null
         * `List<ChosenTarget>` form is still accepted (Kotlin covariance) for
         * callers that already filter targets up-front.
         */
        fun buildNamedTargets(
            requirements: List<TargetRequirement>,
            targets: List<ChosenTarget?>
        ): Map<String, ChosenTarget> {
            val result = mutableMapOf<String, ChosenTarget>()
            var targetIndex = 0
            for (req in requirements) {
                val id = req.id
                if (id != null) {
                    if (req.count == 1) {
                        targets.getOrNull(targetIndex)?.let { result[id] = it }
                    } else {
                        for (i in 0 until req.count) {
                            targets.getOrNull(targetIndex + i)?.let { result["$id[$i]"] = it }
                        }
                    }
                }
                targetIndex += req.count
            }
            return result
        }

        /**
         * Build the execution context for a triggered ability sitting on the stack.
         *
         * Shared by the two places that need to evaluate the ability's own text against the game
         * state: [com.wingedsheep.engine.mechanics.stack.StackResolver] when the ability resolves,
         * and [com.wingedsheep.engine.event.TriggerProcessor] when it resolves a modal
         * `dynamicChooseCount` as the ability goes onto the stack (CR 603.3c). Both must see the
         * same trigger payload — an `xValue`, a `MODES_CHOSEN_ON_TRIGGERING_SPELL` count or a
         * carried pipeline collection that only one of them populated would read as zero/absent.
         */
        fun forTriggeredAbility(
            ability: TriggeredAbilityOnStackComponent,
            targets: List<ChosenTarget> = emptyList(),
            targetRequirements: List<TargetRequirement> = emptyList()
        ): EffectContext = EffectContext(
            sourceId = ability.sourceId,
            controllerId = ability.controllerId,
            granterId = ability.granterId,
            abilityIdentity = ability.abilityIdentity,
            sourceFaceChanges = ability.sourceFaceChanges,
            sourceBattlefieldTimestamp = ability.sourceBattlefieldTimestamp,
            objectReferences = ability.objectReferences,
            targets = targets,
            triggerContext = ability.triggerContext,
            triggeringEntityId = ability.triggerContext?.triggeringEntityId,
            triggeringPlayerId = ability.triggerContext?.triggeringPlayerId,
            xValue = ability.xValue,
            damageDistribution = ability.damageDistribution,
            chosenModes = ability.chosenModes,
            modeTargetsOrdered = ability.modeTargetsOrdered,
            modeTargetRequirements = ability.modeTargetRequirements,
            pipeline = PipelineState(
                namedTargets = buildNamedTargets(targetRequirements, targets) +
                    (ability.carriedPipeline?.namedTargets ?: emptyMap()),
                // Expose a batch trigger's captured permanents (the matching members of a
                // PermanentsEnteredEvent batch) so a ForEachInCollectionEffect payoff can iterate
                // them — "for each of them, create a tapped copy of it" (Kambal). The copy executor
                // reads each entity at resolution, so any that left the battlefield meanwhile no-op.
                storedCollections = (ability.triggerContext?.capturedEntityIds?.takeIf { it.isNotEmpty() }
                    ?.let { mapOf(PipelineState.TRIGGER_CAPTURED_COLLECTION to it) }
                    ?: emptyMap()) + (ability.carriedPipeline?.storedCollections ?: emptyMap()),
                // A `ReflexiveTriggerEffect`'s action half (e.g. `Amass`, a discard) may have stashed
                // subtype groups or scalar values the reflexive effect reads (CR 603.12) — carried
                // across the stack round-trip since this ability builds a fresh context on resolve.
                storedSubtypeGroups = ability.carriedPipeline?.storedSubtypeGroups ?: emptyMap(),
                chosenValues = ability.carriedPipeline?.chosenValues ?: emptyMap(),
                storedNumbers = ability.carriedPipeline?.storedNumbers ?: emptyMap(),
                storedStringLists = ability.carriedPipeline?.storedStringLists ?: emptyMap()
            )
        )
    }
}

internal const val RESOLUTION_CHOSEN_OPPONENT = "resolution.chosenOpponent"
