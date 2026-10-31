package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PutOntoBattlefieldAttachedToChosenContinuation
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.handlers.effects.LibraryPlacement
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome

/**
 * Executor for MoveToZoneEffect.
 * Unified zone-moving effect that consolidates destroy, exile, bounce,
 * shuffle-into-library, put-on-top, etc.
 *
 * Delegates all zone movement to [ZoneTransitionService] for consistent cleanup.
 *
 * @param effectExecutor the registry's recursive executor, used to run an entering permanent's
 *   [com.wingedsheep.sdk.scripting.OnEnterRun] replacement. Required rather than nullable:
 *   a caller that provably never reaches the battlefield passes a throwing stub (the
 *   bounce-to-hand reuse in `ReturnSpellOrPermanentToOwnersHandExecutor`), so if that destination
 *   ever changes it fails loudly instead of silently skipping a rules-required replacement.
 */
class MoveToZoneEffectExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<MoveToZoneEffect> {

    override val effectType: KClass<MoveToZoneEffect> = MoveToZoneEffect::class

    override fun execute(
        state: GameState,
        effect: MoveToZoneEffect,
        context: EffectContext
    ): EffectResult {
        val result = move(state, effect, context)
        return withDepartedPermanent(state, result, context.resolveTarget(effect.target, state))
    }

    /**
     * Report the moved permanent's battlefield-exit snapshot as
     * [EffectResult.updatedDepartedPermanents] when this move took it off the battlefield, so a
     * later instruction of the same resolution reads "that creature" as it last existed there
     * (CR 608.2h) — "Return target creature to its owner's hand. You may have Shapeshifters you
     * control become copies of that creature" (Absorb Identity). The single-target twin of
     * [com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor]'s capture.
     */
    private fun withDepartedPermanent(before: GameState, result: EffectResult, targetId: EntityId?): EffectResult {
        if (targetId == null || targetId !in before.getBattlefield() || targetId in result.state.getBattlefield()) {
            return result
        }
        val snapshot = result.state.getEntity(targetId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent>()
            ?.snapshot
            ?: return result
        if (result.updatedDepartedPermanents.any { it.entityId == targetId }) return result
        return result.copy(updatedDepartedPermanents = result.updatedDepartedPermanents + snapshot)
    }

    private fun move(
        state: GameState,
        effect: MoveToZoneEffect,
        context: EffectContext
    ): EffectResult {
        var context = context
        val targetReference = effect.target
        val targetId = context.resolveTarget(targetReference, state)
            ?: return if (effect.target == com.wingedsheep.sdk.scripting.targets.EffectTarget.Self ||
                effect.target == com.wingedsheep.sdk.scripting.targets.EffectTarget.IterationEntity ||
                effect.target == com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity ||
                effect.target is com.wingedsheep.sdk.scripting.targets.EffectTarget.LibraryTop ||
                (targetReference is com.wingedsheep.sdk.scripting.targets.EffectTarget.BoundVariable &&
                    context.pipeline.declaredTargetNames.any { name ->
                        targetReference.name == name || targetReference.name.startsWith("$name[")
                    })) {
                EffectResult.success(state)
            } else EffectResult.error(state, "No valid target for move to zone")

        // A later instruction of this resolution finds both cards of a departed meld.
        // Reuse the composite continuation machinery so entry choices can pause between cards.
        if (effect.target !is com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity) {
            val parts = context.objectReferences.meldedCards(targetId, state)
            if (parts != null) {
                if (parts.isEmpty()) return EffectResult.success(state)
                return effectExecutor(state, com.wingedsheep.sdk.scripting.effects.CompositeEffect(parts.map {
                    effect.copy(target = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(it))
                }), context)
            }
        }

        // byDestruction delegates to destroyPermanent (handles indestructible)
        if (effect.byDestruction) {
            return destroyPermanent(
                zones, state, targetId,
                lookBackGrants = context.lookBackGrants[targetId]
            )
        }

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card: $targetId")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return EffectResult.error(state, "Cannot determine card owner")

        val currentZone = findEntityZone(state, targetId)
            ?: return EffectResult.error(state, "Card not found in any zone: $targetId")

        // If fromZone is specified, skip the move if the target is not in the expected zone
        if (effect.fromZone != null && currentZone.zoneType != effect.fromZone) {
            return EffectResult.success(state, emptyList())
        }

        // Resolve controller override for "under your control" effects
        val controllerOverride = effect.controllerOverride
        val controllerId = if (controllerOverride != null && effect.destination == Zone.BATTLEFIELD) {
            context.resolveTarget(controllerOverride, state) ?: ownerId
        } else {
            ownerId
        }

        // "<Cards> can't enter the battlefield" (Worms of the Earth, Soulless Jailer). The card simply
        // doesn't enter and stays where it was. The transition service enforces this for every move;
        // asking here too keeps a locked card from prompting for an Aura host or an entry choice.
        if (effect.destination == Zone.BATTLEFIELD &&
            EntryLocks.cantEnter(
                state, targetId, currentZone.zoneType, cardRegistry, zones.predicateEvaluator,
                faceDown = effect.faceDown != null
            )
        ) {
            return EffectResult.success(state)
        }

        // "Enters as a copy" applies first (CR 616.1c): every later question — and whether a
        // replacement sends the card elsewhere instead — is about the card as it would enter.
        if (effect.destination == Zone.BATTLEFIELD && effect.faceDown == null) {
            com.wingedsheep.engine.handlers.effects.copy.EffectCopyEntry.prepare(
                state, effect, context, mapOf(targetId to controllerId), cardRegistry, zones.predicateEvaluator
            )?.let { return it }
        }

        // A card a replacement sends elsewhere instead (Containment Priest's exile) never enters,
        // so it asks none of the entry questions below; the move itself applies the redirect.
        val entersHere = effect.destination == Zone.BATTLEFIELD && effect.faceDown == null &&
            !ZoneMovementUtils.entryRedirected(
                state, targetId, currentZone.zoneType, controllerId, context.entryCopies[targetId],
                cardRegistry, zones.predicateEvaluator, effect.entersWith
            )

        if (entersHere) {
            // "If this would enter, you may [pay] instead" (EntersOnlyIfPaid) — asked once the entry
            // copy is settled (CR 614.12: of the card as it would exist on the battlefield), and
            // before anything else, since it decides whether the card enters at all.
            com.wingedsheep.engine.handlers.effects.EffectEntryPayments.prepare(
                state, effect, context, mapOf(targetId to controllerId), cardRegistry, zones.predicateEvaluator
            )?.let { return it }
        }

        if (entersHere) {
            val prepared = com.wingedsheep.engine.handlers.effects.copy.CopyAuraEntry.prepare(
                state, effect, context, mapOf(targetId to controllerId), cardRegistry, targetFinder, zones.predicateEvaluator)
            prepared.pause?.let { return it }
            context = prepared.context
            if (targetId in context.entryAuraHosts && context.entryAuraHosts[targetId] == null) {
                return EffectResult.success(state)
            }
        }

        // CR 303.4f — an Aura entering the battlefield by any means other than resolving as an
        // Aura spell (here: reanimation / return from graveyard, exile, etc.) has its controller
        // choose what it enchants as it enters. Without that choice the Aura would enter
        // unattached and immediately die to a state-based action (CR 704.5m). Cast Auras attach
        // during stack resolution and never reach this executor, and the explicit
        // "attached to ..." effect has its own executor, so a generic move-to-battlefield of an
        // Aura is always the choose-as-it-enters case.
        if (entersHere && cardComponent.typeLine.isAura && targetId !in context.entryAuraHosts && context.entryCopies[targetId]?.copiedCard == null) {
            return attachAuraOnEnter(state, targetId, cardComponent, controllerId, context)
        }

        // "As this enters, choose …" (CR 614.12a) — asked before the move, of the copied card when
        // the entrant enters as a copy; the transition stamps the answers on arrival.
        if (entersHere) {
            com.wingedsheep.engine.handlers.effects.EffectEntryChoices.prepare(
                state, effect, context, mapOf(targetId to controllerId), cardRegistry, zones.predicateEvaluator
            )?.let { return it }
        }

        // Entry-counter counts are read now, while the card is still where it was — "with X +1/+1
        // counters on it, where X is the number of voyage counters on it" counts the counters it
        // loses on leaving exile (Cosima, God of the Voyage).
        val suppliedCounters = effect.entryCounters.mapNotNull { entry ->
            val count = (entry.count as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount
                ?: zones.predicateEvaluator.amounts.evaluate(state, entry.count, context)
            if (count > 0) com.wingedsheep.engine.handlers.effects.SuppliedEntryCounter(entry.counterType, entry.onlyIf, count) else null
        }

        // Build ZoneEntryOptions based on placement and effect properties
        val entryOptions = buildEntryOptions(effect, cardComponent, controllerId, context.controllerId)
            .copy(lookBackGrants = context.lookBackGrants[targetId], entryCopy = context.entryCopies[targetId], auraHostId = context.entryAuraHosts[targetId],
                entryChoices = context.entryChoices[targetId]?.values.orEmpty(),
                grantedRiotModes = context.entryChoices[targetId]?.riotModes.orEmpty(),
                paidToEnter = com.wingedsheep.engine.handlers.effects.EffectEntryPayments.paid(context, targetId),
                entryCounters = suppliedCounters.takeIf { effect.destination == Zone.BATTLEFIELD }.orEmpty(),
                entryCounterSourceId = context.sourceId,
                entryCounterEffectControllerId = context.controllerId,
                exiledBy = context.controllerId)

        val transitionResult = zones.moveToZone(
            state, targetId, effect.destination, entryOptions, currentZone
        )

        var resultState = transitionResult.state
        val extraEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Apply "enters with counters" replacement effects when a permanent enters the
        // battlefield from a non-stack zone (e.g., reanimation from graveyard, return from
        // exile). Spell resolution applies these in StackResolver, so this path covers
        // everything else. Skip face-down entries: morph creatures enter as 2/2 nameless
        // creatures with no replacement effects from their face-up identity.
        val actualDestZone = transitionResult.actualDestination
        if (actualDestZone == Zone.BATTLEFIELD && effect.faceDown == null && suppliedCounters.isEmpty()) {
            val (counterState, counterEvents) = EntersWithReplacements.applyOnEntry(
                resultState, targetId, controllerId, cardRegistry,
                predicateEvaluator = zones.predicateEvaluator, preEntryZone = currentZone
            )
            resultState = counterState
            extraEvents.addAll(counterEvents)
        }

        if (actualDestZone == Zone.BATTLEFIELD && effect.placement == ZonePlacement.TappedAndAttacking) {
            resultState = settleEnteringAttack(resultState, targetId, controllerId, effect, context)
        }

        // "Exile it with a stash counter on it" — each entry's count of its type on the card once it
        // has landed. Mirrors MoveCollectionEffect.addCounterType; no CountersAddedEvent is emitted
        // because the card isn't a permanent in its new zone, so nothing can trigger off it.
        // An entry's `onlyIf` must match the card in *projected* state where it landed. (Battlefield
        // entries are placed by the zone transition itself, as part of the entry event.)
        if (actualDestZone == effect.destination && actualDestZone != Zone.BATTLEFIELD) {
            for (entry in suppliedCounters) {
                val allowed = entry.onlyIf?.let { filter ->
                    zones.predicateEvaluator.matches(
                        resultState, resultState.projectedState, targetId, filter,
                        com.wingedsheep.engine.handlers.PredicateContext(controllerId = controllerId, sourceId = context.sourceId)
                    )
                } ?: true
                if (allowed) resultState = resultState.updateEntity(targetId) { container ->
                    val existing = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(existing.withAdded(entry.counterType, entry.count))
                }
            }
        }

        // Link exiled card to source permanent via LinkedExileComponent
        if (effect.linkToSource && effect.destination == Zone.EXILE) {
            resultState = com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
                .link(resultState, context, listOf(targetId))
        }

        // Auto-reveal: when a card moves from a publicly visible zone (graveyard/exile)
        // back into a hidden zone (hand/library) or onto the battlefield, emit a
        // CardsRevealedEvent so the UI can surface what card moved and why. The card was
        // already public info, so this just surfaces the move in the reveal overlay.
        //
        // The "revealer" is the controller performing the move, not the card's owner: a
        // reanimation can put an opponent's graveyard card onto *your* battlefield under
        // *your* control (Shark Shredder), and the overlay must read as you doing it, not
        // as the opponent. controllerId only diverges from ownerId for a controller-override
        // move onto the battlefield (see above); returns to hand/library never carry an
        // override, so controllerId == ownerId there and behaviour is unchanged.
        extraEvents.addAll(
            autoRevealForReturn(
                fromZone = currentZone.zoneType,
                wasFaceDown = container.has<FaceDownComponent>(),
                toZone = effect.destination,
                targetId = targetId,
                cardName = cardComponent.name,
                imageUri = cardComponent.imageUri,
                controllerId = controllerId,
                sourceId = context.sourceId,
                state = resultState
            )
        )

        // "As this permanent enters, run [effect]" (OnEnterRun) — the self-replacement
        // PlayLandHandler runs inline for a played land, applied here for every *other* way a card
        // reaches the battlefield: reanimation, a blink or earthbend return from exile. Without it
        // a permanent whose entry choice lives in this replacement — Multiversal Passage's "as
        // this land enters, choose a basic land type" — comes back with the choice never made and
        // no way to make it afterwards. (A library search that puts a card onto the battlefield
        // does NOT land here even for a single card: it routes through MoveCollectionExecutor,
        // which doesn't run this replacement yet.)
        //
        // Runs last so the entry's own events are already collected: the effect may pause for the
        // choice, and the paused result carries them (including the entry ZoneChangeEvent) so the
        // entry's ETB triggers are deferred by the resume path rather than lost. Skipped for
        // face-down entries (CR 708.2 — no abilities) and for a battlefield→battlefield redirect,
        // which is not a new entry. An Aura never reaches here either — attachAuraOnEnter above
        // returns first — so an Aura carrying this replacement would still skip it; no card does
        // today, and wiring it belongs with the choose-a-host continuation, not here.
        if (actualDestZone == Zone.BATTLEFIELD &&
            effect.faceDown == null &&
            currentZone.zoneType != Zone.BATTLEFIELD
        ) {
            val onEnterResult = PermanentEntryReplacements.runOnEnterRunEffect(
                resultState, targetId, controllerId, cardRegistry, effectExecutor,
                resolutionDepth = context.resolutionDepth,
                parentOrigin = context.programOrigin,
            )
            if (onEnterResult != null) {
                resultState = onEnterResult.state
                extraEvents.addAll(onEnterResult.events)
                // One return covers both the paused and the completed case.
                //
                // A rejection is deliberately NOT propagated. The permanent did enter —
                // only the as-enters clause failed — and surfacing an error here would make the
                // enclosing composite treat the whole move as the failed step (CR 609.3: an
                // effect that attempts something impossible does only as much as possible).
                return EffectResult(
                    state = resultState,
                    events = transitionResult.events + extraEvents,
                    outcome = onEnterResult.outcome.takeIf { it is Outcome.Paused } ?: Outcome.Done,
                )
            }
        }

        return EffectResult.success(resultState, transitionResult.events + extraEvents)
    }

    /**
     * Handle an Aura that is being put onto the battlefield by something other than resolving as
     * an Aura spell (CR 303.4g). The controller chooses what it enchants from among everything it
     * can legally enchant (CR 303.4f — targeting restrictions like hexproof/shroud are ignored for
     * this choice). The actual move-and-attach happens in
     * [PutOntoBattlefieldAttachedToChosenContinuation], reused from the explicit "attached to"
     * effect so both paths wire `AttachedToComponent`, the host's `AttachmentsComponent`, and the
     * Aura's continuous/replacement effects identically.
     */
    private fun attachAuraOnEnter(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        controllerId: EntityId,
        context: EffectContext
    ): EffectResult {
        val auraTarget = cardRegistry.getCard(cardComponent.cardDefinitionId)?.script?.auraTarget
        val legalHosts = if (auraTarget == null) emptyList() else targetFinder.findLegalTargets(
            state = state,
            requirement = auraTarget,
            controllerId = controllerId,
            sourceId = cardId,
            ignoreTargetingRestrictions = true
        )

        // No legal host — the Aura can't enter and stays in its current zone (CR 303.4g).
        if (legalHosts.isEmpty()) {
            return EffectResult.success(state)
        }

        val cardName = cardComponent.name
        val decision = { decisionId: String -> ChooseTargetsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose what $cardName attaches to",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(
                TargetRequirementInfo(
                    index = 0,
                    description = "what $cardName attaches to",
                    minTargets = 1,
                    maxTargets = 1
                )
            ),
            legalTargets = mapOf(0 to legalHosts)
        ) }
        val continuation = PutOntoBattlefieldAttachedToChosenContinuation(
            cardId = cardId,
            controllerId = controllerId
        )
        return EffectResult.from(state.suspendForDecision(decision, continuation, emptyList()))
    }

    private fun autoRevealForReturn(
        fromZone: Zone,
        wasFaceDown: Boolean,
        toZone: Zone,
        targetId: com.wingedsheep.sdk.model.EntityId,
        cardName: String,
        imageUri: String?,
        controllerId: com.wingedsheep.sdk.model.EntityId,
        sourceId: com.wingedsheep.sdk.model.EntityId?,
        state: GameState
    ): List<com.wingedsheep.engine.core.GameEvent> {
        // Exile identities must be read before the move clears the face-down marker.
        val isFromPublic = fromZone == Zone.GRAVEYARD || (fromZone == Zone.EXILE && !wasFaceDown)
        val isToReturnDestination =
            toZone == Zone.HAND || toZone == Zone.LIBRARY || toZone == Zone.BATTLEFIELD
        if (!isFromPublic || !isToReturnDestination) return emptyList()

        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        return listOf(
            CardsRevealedEvent(
                revealingPlayerId = controllerId,
                cardIds = listOf(targetId),
                cardNames = listOf(cardName),
                imageUris = listOf(imageUri),
                source = sourceName,
                // The controller performed the move themselves — they already know what/where
                // it is. Only the other player needs the reveal overlay to surface the
                // transition (e.g. their card being reanimated onto their opponent's board).
                revealToSelf = false,
                fromZone = fromZone,
                toZone = toZone
            )
        )
    }

    /**
     * Finish a tapped-and-attacking entry once the permanent has landed (and taken its entering
     * counters, so a loyalty-gated animation such as Kaito, Bane of Nightmares' is visible):
     *  - [EnteringAttackDefender.RETURNED_ATTACKER] re-aims the attack at the defender the creature
     *    returned to pay the ability's cost was attacking (ninjutsu, CR 702.49c). If that defender
     *    is no longer a legal attack target the permanent enters but is never attacking
     *    (CR 506.3c), and likewise when the cost returned no attacker.
     *  - A permanent that isn't a creature as it enters is never attacking (CR 506.3a).
     */
    private fun settleEnteringAttack(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        effect: MoveToZoneEffect,
        context: EffectContext,
    ): GameState {
        val defender = when (effect.attackDefender) {
            EnteringAttackDefender.RETURNED_ATTACKER -> context.returnedAttackerDefenders.firstOrNull()?.takeIf {
                com.wingedsheep.engine.mechanics.combat.CombatDefenders.isLegalEnteringAttackTarget(state, controllerId, it)
            }
            else -> state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()?.defenderId
        }
        if (defender == null || !state.projectedState.isCreature(entityId)) {
            return state.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
            }
        }
        if (effect.attackDefender != EnteringAttackDefender.RETURNED_ATTACKER) return state
        val updated = state.updateEntity(entityId) {
            it.with(
                com.wingedsheep.engine.state.components.combat.AttackingComponent(
                    defender,
                    defendingPlayerId = com.wingedsheep.engine.mechanics.combat.CombatDefenders.defendingPlayerOf(state, defender),
                )
            )
        }
        return com.wingedsheep.engine.mechanics.combat.AttackedPermanents.markAttacked(updated, defender)
    }

    /**
     * Build [ZoneEntryOptions] from the effect's placement and properties.
     */
    private fun buildEntryOptions(
        effect: MoveToZoneEffect,
        cardComponent: CardComponent,
        controllerId: com.wingedsheep.sdk.model.EntityId,
        moverId: com.wingedsheep.sdk.model.EntityId
    ): ZoneEntryOptions {
        val libraryPlacement = when {
            effect.positionFromTop != null && effect.destination == Zone.LIBRARY ->
                LibraryPlacement.NthFromTop(effect.positionFromTop!!)
            effect.placement == ZonePlacement.Top -> LibraryPlacement.Top
            effect.placement == ZonePlacement.Bottom -> LibraryPlacement.Bottom
            effect.placement == ZonePlacement.Shuffled -> LibraryPlacement.Shuffled
            else -> LibraryPlacement.Top
        }

        // Derive turn-up data for a face-down battlefield entry (morph cost vs. manifest mana cost).
        val faceDownMode = effect.faceDown
        val isBattlefieldFaceDown = faceDownMode != null && effect.destination == Zone.BATTLEFIELD
        val morphData = if (isBattlefieldFaceDown) {
            com.wingedsheep.engine.handlers.effects.FaceDownTurnUp.dataFor(
                cardRegistry.getCard(cardComponent.cardDefinitionId),
                cardComponent.cardDefinitionId,
                faceDownMode
            )
        } else null

        return ZoneEntryOptions(
            controllerId = controllerId,
            libraryPlacement = libraryPlacement,
            tapped = effect.placement == ZonePlacement.Tapped,
            tappedAndAttacking = effect.placement == ZonePlacement.TappedAndAttacking,
            faceDown = isBattlefieldFaceDown,
            morphData = morphData,
            faceDownMode = if (isBattlefieldFaceDown) faceDownMode else null,
            entersWith = effect.entersWith,
            // Single-card sibling of the collection path: the player resolving the effect chose
            // this card and saw where it went, so they keep knowing it. The audience policy —
            // including "out of a public zone means the whole table knows" — is
            // LibraryRevealUtils.placementAudience's, not this executor's.
            libraryMoverId = moverId
        )
    }

    private fun findEntityZone(state: GameState, entityId: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.engine.state.ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
