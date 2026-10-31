package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.library.markSuspended
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.composite.PreTargetedEffectContext
import com.wingedsheep.engine.handlers.effects.composite.processPreTargetedEffectQueue
import com.wingedsheep.engine.handlers.effects.permanent.types.restoreDfcFrontFace
import com.wingedsheep.engine.handlers.effects.permanent.types.returnDfcFace
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.CipherEncodeChoiceComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.state.permissions.removeMayPlayPermissionsForCard
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Resolves an instant or sorcery spell (CR 608.2c–n): runs its effect (and any spliced text,
 * CR 702.47b), then moves the card to its destination — graveyard, or the exile / library / battlefield
 * that flashback, rebound, an Adventure, an Omen, or a resolution rider sends it to instead.
 */
internal class NonPermanentSpellResolver(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val effects: EffectExecutorRegistry,
    private val predicateEvaluator: PredicateEvaluator,
    private val spliceTargetValidator: TargetValidator
) {
    /**
     * The spliced text of [spellComponent]'s spell as a drain queue (CR 702.47b) — one entry per
     * spliced card, in the caster's chosen order, each carrying its own target slice and requirements.
     *
     * A spliced card contributes its *rules text*, so what is queued is its `spellEffect`; a splice
     * card with no spell effect (nothing splice-able) simply drops out.
     */
    private fun buildSpliceEntries(
        spellComponent: SpellOnStackComponent,
        boundRequirements: List<TargetRequirement>,
        splicedAlignedTargets: List<ChosenTarget?>,
    ): List<PreTargetedEffectEntry> {
        // Each spliced card's requirements as the cast bound them — the tail of the recorded list,
        // in splice order — so a partly filled "up to N" spliced group names the targets it holds.
        val printed = spellComponent.splicedCardNames.map { cardRegistry.getCard(it)?.script?.targetRequirements.orEmpty() }
        var cursor = boundRequirements.size - printed.sumOf { it.size }
        val bound = printed.map { reqs ->
            (if (cursor >= 0) boundRequirements.subList(cursor, cursor + reqs.size) else reqs).also { cursor += reqs.size }
        }
        // Each spliced card's slice of the spell's resolution-validated slots (CR 608.2b): a target
        // that became illegal is a null in its slot, so the spliced text still acts on the targets
        // that remain legal, and on whatever it does without a target.
        var slot = 0
        val alignedSlices = bound.map { reqs ->
            val count = reqs.sumOf { it.count }
            splicedAlignedTargets.drop(slot).take(count).also { slot += count }
        }
        return spellComponent.splicedCardNames.mapIndexedNotNull { index, name ->
            val splicedDef = cardRegistry.getCard(name) ?: return@mapIndexedNotNull null
            val effect = splicedDef.script.spellEffect ?: return@mapIndexedNotNull null
            val aligned = alignedSlices[index]
            PreTargetedEffectEntry(
                effect = effect,
                targets = aligned.filterNotNull(),
                targetRequirements = bound[index],
                alignedTargets = aligned
            )
        }
    }

    /**
     * Resolve a non-permanent spell - execute effects, put in graveyard.
     *
     * CR 608.2c: the spell's instructions are followed in order — its own effect first, then any
     * spliced text (CR 702.47b) — and only then (CR 608.2n) is the card moved off the stack by
     * [finishNonPermanentSpell]. When an effect pauses, the pre-pushed
     * [FinishResolvingSpellContinuation] performs that last step once the decision resolves.
     */
    fun resolveNonPermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        targets: List<ChosenTarget>,
        // Parallel to the originally-chosen targets, with `null` in slots whose target
        // was dropped by 608.2b validation. Used for [EffectContext.buildNamedTargets]
        // so BoundVariable lookups for now-illegal targets resolve to null and fizzle,
        // rather than shifting onto a later still-valid target.
        alignedTargets: List<ChosenTarget?> = targets,
    ): ExecutionResult {
        // resolveTop removed the item from the priority stack; a resolving spell itself
        // remains a stack object until its effects finish, below any spell those effects cast.
        var newState = if (spellId !in state.stack) state.copy(stack = state.stack + spellId) else state
        val events = mutableListOf<GameEvent>()

        val resolvedCardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        val spellEffect = resolvingSpellEffect(state, spellId, spellComponent, cardComponent, resolvedCardDef)
            ?.let { withCipherEncodeStep(state, spellId, resolvedCardDef, it) }
        // Splice (CR 702.47): the spliced cards' text is a tail that runs after the main spell's own
        // effects (CR 702.47b). Its targets were appended to the end of the flat list at cast time, so
        // the same tail is peeled off here — the main spell must see only its own targets, or an effect
        // that consumes "all targets" would swallow the spliced card's as well.
        // A spell with no effect of its own can't be a splice host in practice (a splice card is
        // spliced onto a spell that has text), so the tail lives inside the `spellEffect != null` guard.
        val splicedRequirementCount = spellComponent.splicedCardNames.sumOf { name ->
            cardRegistry.getCard(name)?.script?.targetRequirements?.size ?: 0
        }
        val allTargetRequirements = state.getEntity(spellId)?.get<TargetsComponent>()?.targetRequirements ?: emptyList()
        val splicedSlotCount = SpliceCasts
            .splicedTargetSlotCounts(spellComponent.splicedCardNames, cardRegistry, allTargetRequirements).sum()
        val spliceEntries = buildSpliceEntries(spellComponent, allTargetRequirements, alignedTargets.takeLast(splicedSlotCount))

        val fusedDefinition = resolvedCardDef?.takeIf { spellComponent.castFused }
        if (spellEffect != null || fusedDefinition != null) {
            // Requirements are never filtered, so the tail comes straight off the end.
            val targetRequirements = allTargetRequirements.dropLast(splicedRequirementCount)
            // The tail is dropped from `alignedTargets`, NOT from `targets`: only the aligned list is
            // position-preserving (null wherever 608.2b dropped a target), so it is the one whose last
            // `splicedSlotCount` entries are reliably the spliced cards'. `targets` is the already-
            // filtered, shorter list — dropping from *it* would eat a main-spell target whenever any
            // target had been dropped, silently shifting positional references like ContextTarget(n).
            // The main spell's own live targets are then just its aligned slots that survived.
            // Both expressions are deliberately identity when nothing was spliced.
            val mainAlignedTargets =
                if (splicedSlotCount == 0) alignedTargets else alignedTargets.dropLast(splicedSlotCount)
            val mainTargets =
                if (splicedSlotCount == 0) targets else mainAlignedTargets.filterNotNull()
            val context = buildSpellEffectContext(
                state, spellId, spellComponent, resolvedCardDef, mainTargets, mainAlignedTargets, targetRequirements
            )

            val finishing = FinishResolvingSpellContinuation(
                spellObject = state.objectRef(spellId)!!,
                spellComponent = spellComponent,
                cardComponent = cardComponent,
            )
            newState = newState.pushContinuation(finishing)

            val effectResult = if (fusedDefinition != null) {
                val entries = fusedEntries(state, spellId, fusedDefinition, targetRequirements, mainAlignedTargets)
                // Each half runs as a root of its own, not as a position in the spell's effect.
                val halves = context.copy(programOrigin = com.wingedsheep.engine.core.ProgramOrigin.BuiltBy(
                    com.wingedsheep.sdk.core.Keyword.FUSE.name, context.programOrigin))
                processPreTargetedEffectQueue(newState, entries + spliceEntries,
                    PreTargetedEffectContext(baseContext = halves, controllerId = context.controllerId,
                        sourceId = spellId, sourceName = cardComponent?.name, xValue = context.xValue,
                        triggeringEntityId = null, objectReferences = context.objectReferences),
                    effects::execute, spliceTargetValidator, emptyList())
            } else runMainEffectThenSplice(
                newState, spellId, spellComponent, cardComponent, spellEffect!!, context, spliceEntries
            )

            if (effectResult.outcome is Outcome.Paused) {
                // The finalizer is below all effect and splice frames; no zone change yet.
                return ExecutionResult.propagatePause(effectResult.state, events + effectResult.events)
            }
            if (effectResult.state.pendingSubgame != null) {
                // The spell created a subgame (CR 729). It finishes resolving only once the subgame
                // is over and the rest of its text has run (CR 729.5), so it stays on the stack and
                // the finalizer waits, as it does beneath a question.
                return ExecutionResult.success(effectResult.state, events + effectResult.events)
            }

            // Always apply state changes from effect execution, even on partial
            // failure. Per MTG rules, when a spell resolves, you do as much as
            // possible. Partial state changes (e.g., first target destroyed but
            // second target missing) should be preserved.
            // The finalizer ran inline, so drop the frame pre-pushed for the paused path. There is
            // at most one per resolving spell object, which is what identifies it now that automatic
            // work carries no routing ID.
            newState = effectResult.newState.copy(continuationStack = effectResult.newState.continuationStack
                .filterNot { it is FinishResolvingSpellContinuation && it.spellObject == finishing.spellObject })
            events.addAll(effectResult.events)
        }

        val completed = if (state.objectRef(spellId)?.let(newState::isCurrentObject) == true)
            finishNonPermanentSpell(newState, spellId, spellComponent, cardComponent)
        else ExecutionResult.success(newState)
        return completed.copy(events = events + completed.events)
    }

    private fun fusedEntries(
        state: GameState,
        spellId: EntityId,
        definition: com.wingedsheep.sdk.model.CardDefinition,
        requirements: List<TargetRequirement>,
        alignedTargets: List<ChosenTarget?>,
    ): List<PreTargetedEffectEntry> {
        var requirementIndex = 0
        var targetIndex = 0
        val text = TextChanges.forSpell(state, spellId)
        return definition.cardFaces.mapNotNull { face ->
            val bound = requirements.drop(requirementIndex).take(face.script.targetRequirements.size)
            requirementIndex += bound.size
            val count = bound.sumOf { it.count }
            val slice = alignedTargets.drop(targetIndex).take(count)
            targetIndex += count
            val effect = face.script.spellEffect ?: return@mapNotNull null
            // A half still does as much as possible when its target is illegal; continue into the other half.
            PreTargetedEffectEntry(com.wingedsheep.sdk.scripting.effects.CompositeEffect(listOf(text?.let { effect.applyTextReplacement(it) } ?: effect)),
                slice.filterNotNull(), bound, alignedTargets = slice)
        }
    }

    /** The context the main spell's effect runs in: its own (non-spliced) targets and every cast-time choice. */
    private fun buildSpellEffectContext(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        resolvedCardDef: com.wingedsheep.sdk.model.CardDefinition?,
        mainTargets: List<ChosenTarget>,
        mainAlignedTargets: List<ChosenTarget?>,
        targetRequirements: List<TargetRequirement>
    ): EffectContext =
        EffectContext(
            sourceId = spellId,
            // The spell's effect runs as a program whose origin is this resolving stack object.
            programOrigin = state.objectRef(spellId)?.let { com.wingedsheep.engine.core.ProgramOrigin.Resolving(it) },
            objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                captured = true, origin = state.objectRef(spellId), source = state.objectRef(spellId),
                resolutionKey = "$spellId:${state.objectRef(spellId)?.generation}",
            ),
            controllerId = spellComponent.casterId,
            targets = mainTargets,
            // Position-preserving view (null in slots dropped by 608.2b) so positional
            // references — ContextTarget(n), ContextPlayer(n) —
            // resolve by ORIGINAL slot and don't shift onto a later still-valid target.
            alignedTargets = mainAlignedTargets,
            // A pay-X-life additional cost (AdditionalCost.PayXLife, e.g. Vicious Rivalry) feeds
            // its declared X through the same X slot read by DynamicAmount.XValue and the
            // ManaValue*X predicates. Such a card never also carries an {X} mana cost, so
            // coalescing is unambiguous (CR 601.2b — the value is locked in as the spell is cast).
            xValue = spellComponent.xValue ?: spellComponent.additionalCostPayXLifeAmount,
            totalManaSpent = spellComponent.manaSpentWhite + spellComponent.manaSpentBlue +
                spellComponent.manaSpentBlack + spellComponent.manaSpentRed +
                spellComponent.manaSpentGreen + spellComponent.manaSpentColorless,
            manaSpentOnXByColor = spellComponent.manaSpentOnXByColor,
            manaSpent = com.wingedsheep.engine.mechanics.mana.SpentMana(
                white = spellComponent.manaSpentWhite, blue = spellComponent.manaSpentBlue,
                black = spellComponent.manaSpentBlack, red = spellComponent.manaSpentRed,
                green = spellComponent.manaSpentGreen, colorless = spellComponent.manaSpentColorless,
            ),
            declaredCostSlot = spellComponent.declaredCostSlot,
            wasBlightPaid = spellComponent.wasBlightPaid,
            wasWaterbendPaid = spellComponent.wasWaterbendPaid,
            wasSneaked = spellComponent.wasSneaked,
            wasWebSlung = spellComponent.wasWebSlung,
            wasMayhem = spellComponent.wasMayhem,
            sacrificedPermanents = spellComponent.sacrificedPermanents,
            discardedAsCostCards = spellComponent.discardedAsCostCards,
            revealedAsCostSnapshots = spellComponent.revealedAsCostSnapshots,
            exiledAsCostCards = spellComponent.exiledAsCostCards,
            exiledAsCostSnapshots = spellComponent.exiledAsCostSnapshots,
            tappedPermanents = spellComponent.tappedPermanents,
            chosenEntitySnapshots = spellComponent.chosenEntitySnapshots,
            damageDistribution = spellComponent.damageDistribution,
            chosenModes = spellComponent.chosenModes,
            modeTargetsOrdered = spellComponent.modeTargetsOrdered,
            modeTargetRequirements = spellComponent.modeTargetRequirements,
            // A repeated mode can bind a different number of targets on each occurrence.
            // The flat requirements already carry those announced counts, including zero.
            modeTargetRequirementsOrdered = spellComponent.chosenModes.takeIf { modes ->
                modes.all { it in spellComponent.modeTargetRequirements }
            }?.let { modes ->
                var cursor = 0
                modes.map { mode ->
                    val size = spellComponent.modeTargetRequirements[mode].orEmpty().size
                    targetRequirements.drop(cursor).take(size).also { cursor += size }
                }
            }.orEmpty(),
            chosenCreatureType = spellComponent.chosenCreatureType,
            exiledCardCount = spellComponent.exiledCardCount,
            additionalCostBlightAmount = spellComponent.additionalCostBlightAmount,
            castFromZone = spellComponent.castFromZone,
            pipeline = PipelineState(
                // Use the positionally-aligned validated list so a sub-effect that
                // references a target dropped by 608.2b through its BoundVariable id
                // resolves to null and fizzles (CR 608.2b).
                declaredTargetNames = targetRequirements.mapNotNull { it.id }.toSet(),
                namedTargets = EffectContext.buildNamedTargets(targetRequirements, mainAlignedTargets),
                storedCollections = buildBeheldStoredCollections(spellComponent.beheldCards, resolvedCardDef)
            )
        )

    /**
     * Run the main spell's effect, then — if it finished without pausing — the spliced text inline
     * (CR 702.47b), so the whole resolution stays one result.
     */
    private fun runMainEffectThenSplice(
        newState: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        spellEffect: Effect,
        context: EffectContext,
        spliceEntries: List<PreTargetedEffectEntry>
    ): EffectResult {
        // Pre-push the splice tail so it runs whether the main spell's effect finishes here or
        // pauses for a decision of its own — the frame sits beneath the inner decision's frames
        // and auto-resumes once they finish (CR 702.47b: main spell first, then the spliced text).
        val stateForMainEffect = if (spliceEntries.isNotEmpty()) {
            newState.pushContinuation(
                SpliceTailContinuation(
                    controllerId = spellComponent.casterId,
                    sourceId = spellId,
                    sourceName = cardComponent?.name,
                    remainingEntries = spliceEntries,
                    objectReferences = context.objectReferences
                )
            )
        } else newState

        var effectResult = effects.execute(stateForMainEffect, spellEffect, context)

        // Main spell done and nothing paused — pop the pre-pushed frame and run the spliced text
        // inline, so the whole resolution stays one ExecutionResult.
        if (spliceEntries.isNotEmpty() && effectResult.outcome !is Outcome.Paused && effectResult.error == null) {
            val (_, afterPop) = effectResult.state.popContinuation()
            val tail = processPreTargetedEffectQueue(
                state = afterPop,
                entries = spliceEntries,
                ctx = PreTargetedEffectContext(
                    controllerId = spellComponent.casterId,
                    sourceId = spellId,
                    sourceName = cardComponent?.name,
                    xValue = null,
                    triggeringEntityId = null,
                    objectReferences = context.objectReferences.authorize(effectResult.events)
                ),
                effectExecutor = effects::execute,
                targetValidator = spliceTargetValidator,
                accumulatedEvents = effectResult.events
            )
            effectResult = tail
        }
        return effectResult
    }

    /** Finish only the captured resolving spell, never a later visit of the same card. */
    fun finishResolvingSpell(state: GameState, continuation: FinishResolvingSpellContinuation): ExecutionResult {
        val result = if (state.isCurrentObject(continuation.spellObject) &&
            state.logicalZone(continuation.spellObject.entityId)?.zoneType == Zone.STACK
        ) finishNonPermanentSpell(state, continuation.spellObject.entityId,
            continuation.spellComponent, continuation.cardComponent)
        else ExecutionResult.success(state)
        return result.copy(events = result.events + ResolvedEvent(continuation.spellObject.entityId,
            continuation.cardComponent?.name ?: "Unknown"))
    }

    private fun finishNonPermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
    ): ExecutionResult {
        if (state.logicalZone(spellId)?.zoneType != Zone.STACK) return ExecutionResult.success(state)
        var newState = if (spellId in state.stack) state.copy(stack = state.stack.filterNot { it == spellId }) else state
        val events = mutableListOf<GameEvent>()
        // Rule 112.3b: a copy of a spell ceases to exist when it leaves the stack —
        // it does not go to a graveyard or exile.
        val isCopy = newState.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            newState = newState.removeEntity(spellId)
            return ExecutionResult.success(newState, events)
        }

        // Move to graveyard (or exile if selfExileOnResolve, flashback, or AfterResolveDestinationComponent)
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        // For a cast face (Adventure / modal DFC), "Exile <name>." lives on the face's script.
        val resolvedScript = spellComponent.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script }
            ?: cardDef?.script

        // Esper Origins: a spell cast from a graveyard is put onto the battlefield transformed
        // instead of going to the graveyard. Gated on the same graveyard cast as the flashback
        // exile below and takes precedence over it. Falls through to the normal destination if the
        // card can't enter transformed (non-DFC or non-permanent back face — official ruling).
        val returnTransformedSpec = resolvedScript?.returnTransformedFromGraveyardOnResolve
        if (returnTransformedSpec != null && spellComponent.castFromZone == Zone.GRAVEYARD) {
            val transformed = resolveSelfToBattlefieldTransformed(
                newState, spellId, returnTransformedSpec.counters, events
            )
            if (transformed != null) {
                return ExecutionResult.success(transformed, events)
            }
        }

        val cipherCreatureId = newState.getEntity(spellId)?.get<CipherEncodeChoiceComponent>()?.creatureId
        val destination = decideDestination(state, newState, spellId, spellComponent, cardDef, resolvedScript)
        val intendedDestination = destination.intendedZone

        // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers
        // exiles cards that would go to your graveyard from anywhere).
        val redirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
            newState, spellId, Zone.STACK, intendedDestination,
            predicateEvaluator = predicateEvaluator
        )
        val destinationZone = redirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destinationZone)

        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TextReplacementComponent>()
                .without<TargetsComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostReductionComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent>()
                .without<AfterResolveDestinationComponent>()
                .without<CipherEncodeChoiceComponent>()
        }
        newState = newState.removeMayPlayPermissionsForCard(spellId)
        newState = newState.addToZone(destZoneKey, spellId)
        // CR 712.8a — a card cast transformed with an instant or sorcery back face leaves the stack
        // here, so it turns back to its front face here.
        newState = restoreDfcFrontFace(newState, cardRegistry, spellId)
        newState = com.wingedsheep.engine.mechanics.SplitSpellCasts.end(newState, spellId)
        if (destinationZone == Zone.GRAVEYARD) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                .recordSpellGraveyardArrival(newState, spellId, ownerId)
        }
        val destinationObject = newState.objectRef(spellId)
        newState = applyOwnDestinationRiders(
            state, newState, spellId, spellComponent, cardComponent, ownerId, resolvedScript, destinationZone,
            destination, events
        )
        newState = applyAfterResolveAndRedirectRiders(
            newState, spellId, cardComponent, ownerId, destinationZone, destination, redirect, events
        )
        // Cipher (CR 702.99a): the card went from the stack straight to exile — encode it there.
        // A redirect that sent it anywhere else leaves nothing encoded.
        if (cipherCreatureId != null && destinationZone == Zone.EXILE) {
            newState = encodeOnCreature(newState, spellId, cipherCreatureId, cardComponent?.name ?: "Unknown")
        }

        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent?.name ?: "Unknown",
                Zone.STACK,
                destinationZone,
                ownerId, oldObject = state.objectRef(spellId), newObject = destinationObject
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Where a resolved instant or sorcery is headed (CR 608.2n) before `RedirectZoneChange`
     * replacements, and which of the card's own replacements put it there.
     */
    private data class SpellDestination(
        val intendedZone: Zone,
        val exileAfterResolveComp: AfterResolveDestinationComponent?,
        val adventureFaceExile: Boolean,
        val omenFaceShuffle: Boolean,
        val selfShuffleIntoLibrary: Boolean,
        val reboundExile: Boolean,
        /** Time counters the card's own "exile with N time counters" clause puts on it (0 = none). */
        val selfSuspendCounters: Int,
        /**
         * Whether a [AfterResolveDestinationComponent.exiledRider]'s "If you do, …" follows: only when
         * the rider really replaced a trip to the graveyard. A spell whose own instructions exile it
         * (or shuffle it away) was never headed for a graveyard, so nothing follows.
         */
        val exiledRiderFollows: Boolean,
    )

    /**
     * Pick the resolved spell's destination (CR 608.2n) from the replacements that apply to it.
     * [state] is the state the spell began finishing in; [newState] has it off the stack.
     */
    private fun decideDestination(
        state: GameState,
        newState: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        resolvedScript: com.wingedsheep.sdk.model.CardScript?
    ): SpellDestination {
        // Cipher (CR 702.99a): the spell's own last instruction exiles the card. That is not a
        // replacement of "put it into its owner's graveyard", so the graveyard riders below
        // (rebound, an AfterResolveDestination rider) have nothing to replace, and flashback's
        // "exile it instead" agrees with it.
        val cipherExile = newState.getEntity(spellId)?.has<CipherEncodeChoiceComponent>() == true
        val selfExile = resolvedScript?.selfExileOnResolve == true
        // A graveyard cast paid with flashback or beam me up (the recorded cost — see
        // StackExitRider), or one with flashback (printed or granted — Archmage's Newt) or Harmonize
        // (printed or granted — Songcrafter Mage), exiles on resolution instead of returning to the
        // graveyard.
        val flashbackExile = StackExitRider.paidGraveyardExileCost(spellComponent) ||
            spellComponent.castFromZone == Zone.GRAVEYARD &&
            (FlashbackGrants.effectiveFlashback(
                state, spellId, cardDef, spellComponent.casterId, cardRegistry, predicateEvaluator
            ) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        // "Exile <name> with N time counters on it." is the spell's own last instruction, like
        // cipher's: a resolving spell that suspends itself is never put into a graveyard, so a
        // graveyard-worded rider (Quistis Trepe, Kylox's Voltstrider) has nothing to replace.
        val selfSuspendCounters = if (selfExile) resolvedScript?.selfExileTimeCounters ?: 0 else 0
        val exileAfterResolveComp = newState.getEntity(spellId)?.get<AfterResolveDestinationComponent>()
            ?.takeUnless { cipherExile || selfSuspendCounters > 0 }
        // Adventure face (CR 715.3d): when an Adventure resolves, exile it instead of putting
        // it in its owner's graveyard, and grant the caster permission to cast it as the
        // creature spell while it remains exiled.
        val adventureFaceExile = cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE &&
            spellComponent.faceIndex != null
        // Omen face (Tarkir: Dragonstorm): when an Omen resolves, shuffle it into its owner's
        // library instead of putting it in the graveyard. No cast-from-exile linkage.
        val omenFaceShuffle = cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.OMEN &&
            spellComponent.faceIndex != null
        // "Shuffle <name> into its owner's library." printed on the card itself (the Mirrodin
        // Besieged Zenith cycle). Same seam as selfExile — it replaces the CR 608.2n destination —
        // but lands in the library shuffled rather than in exile.
        val selfShuffleIntoLibrary = resolvedScript?.selfShuffleIntoLibraryOnResolve == true
        // Rebound (CR 702.88): a spell cast from hand that has rebound (printed or granted) exiles
        // on resolution instead of going to the graveyard, and arms a next-upkeep free recast.
        // Rebound replaces "put it into its owner's graveyard" too, so a spell that exiles itself
        // (plainly, or suspended with time counters) gives it nothing to replace.
        val reboundExile = !cipherExile && !selfExile && spellComponent.castFromZone == Zone.HAND &&
            spellHasRebound(newState, spellId, cardDef)
        // Not a plain priority order, because the underlying replacements aren't totally ordered:
        // the rider loses to the printed self-shuffle clause, the self-shuffle clause loses to
        // flashback, and flashback loses to the rider. What breaks the cycle is *what each
        // replacement is worded to replace*, which is what the guards below encode:
        //
        //  - the rider and rebound/adventure/omen all replace "…instead of putting it into its
        //    owner's **graveyard**", so a spell that shuffles itself into its owner's library
        //    gives them nothing to replace;
        //  - flashback and harmonize replace "…instead of putting it **anywhere else** any time it
        //    would leave the stack", which covers the library move too, so they still apply.
        //
        // Countered and fizzled spells really are put into a graveyard, and those paths don't read
        // the self-shuffle flag at all, so every clause here applies to them as usual.
        //
        // Buyback (CR 702.27a) replaces "into that player's graveyard" with its owner's hand. Against
        // another graveyard-worded replacement (a rider, rebound) CR 616.1 lets the spell's controller
        // choose; the engine doesn't ask and always applies buyback, the choice buyback was paid for
        // (a rare pairing — e.g. a buyback spell cast through Kylox's Voltstrider). Flashback's "anywhere else" still applies
        // after buyback has, so a flashed-back spell is exiled either way.
        val boughtBack = spellComponent.declaredCostSlot == ChoiceSlot.BOUGHT_BACK && !flashbackExile &&
            !cipherExile && !selfExile && !selfShuffleIntoLibrary
        val intendedDestination = when {
            cipherExile -> Zone.EXILE
            boughtBack -> Zone.HAND
            // The rider is the most specific instruction on this one spell, so it outranks the
            // card-intrinsic exile reasons below rather than being OR'd into them — it is the only
            // one that can send the card somewhere other than exile (Kylox's Voltstrider — "put it
            // on the bottom of its owner's library instead"). The guard is the graveyard wording.
            exileAfterResolveComp != null && !selfShuffleIntoLibrary -> exileAfterResolveComp.zone
            // Flashback (CR 702.34a) / harmonize (CR 702.180a) — "anywhere else". Above the printed
            // clause, below the rider, which leaves the pre-existing rider-vs-flashback precedence
            // exactly as it was.
            flashbackExile -> Zone.EXILE
            selfShuffleIntoLibrary -> Zone.LIBRARY
            selfExile || adventureFaceExile || reboundExile -> Zone.EXILE
            omenFaceShuffle -> Zone.LIBRARY
            else -> Zone.GRAVEYARD
        }
        return SpellDestination(
            intendedZone = intendedDestination,
            exileAfterResolveComp = exileAfterResolveComp,
            adventureFaceExile = adventureFaceExile,
            omenFaceShuffle = omenFaceShuffle,
            selfShuffleIntoLibrary = selfShuffleIntoLibrary,
            reboundExile = reboundExile,
            selfSuspendCounters = if (intendedDestination == Zone.EXILE && !cipherExile) selfSuspendCounters else 0,
            exiledRiderFollows = exileAfterResolveComp != null && !selfExile && !selfShuffleIntoLibrary,
        )
    }

    /**
     * The card's own after-move riders: Paradigm's marker, rebound's recast, an Adventure's
     * cast-from-exile permission, and the shuffle an Omen or a self-shuffling spell asks for.
     */
    private fun applyOwnDestinationRiders(
        state: GameState,
        current: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        ownerId: EntityId,
        resolvedScript: com.wingedsheep.sdk.model.CardScript?,
        destinationZone: Zone,
        destination: SpellDestination,
        events: MutableList<GameEvent>
    ): GameState {
        var newState = current
        val reboundExile = destination.reboundExile
        val adventureFaceExile = destination.adventureFaceExile
        val omenFaceShuffle = destination.omenFaceShuffle
        val selfShuffleIntoLibrary = destination.selfShuffleIntoLibrary
        // Paradigm (Secrets of Strixhaven): tag the just-exiled spell so the engine synthesizes its
        // recurring precombat-main free-recast ability (Paradigm.recastAbility). The marker is the
        // gate — a Lesson exiled by any other path carries no marker and so never recurs.
        if (destinationZone == Zone.EXILE && resolvedScript?.paradigm == true) {
            newState = newState.updateEntity(spellId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.ParadigmComponent)
            }
        }

        // "Exile <name> with N time counters on it." (Arc Blade, Suspended Sentence): the card's own
        // exile carries the counters, and since it has suspend it lands suspended (CR 702.62b) —
        // its owner, the only player who may play it (CR 702.62a), gets the countdown. Flashback's
        // "exile it instead" agrees with the clause; a RedirectZoneChange that sent it elsewhere
        // leaves nothing to put counters on.
        val exileTimeCounters = destination.selfSuspendCounters
        if (exileTimeCounters > 0 && destinationZone == Zone.EXILE) {
            newState = newState.updateEntity(spellId) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withAdded(CounterType.TIME, exileTimeCounters))
            }
            events.add(
                CountersAddedEvent(
                    entityId = spellId,
                    counterType = CounterType.TIME,
                    amount = exileTimeCounters,
                    entityName = cardComponent?.name ?: "Unknown",
                    firstThisTurn = false,
                    placedBy = spellComponent.casterId
                )
            )
            newState = newState.markSuspended(spellId, EffectContext(sourceId = spellId, controllerId = ownerId))
        }

        // Rebound (CR 702.88a): arm the caster's next-upkeep free recast of the just-exiled card.
        if (reboundExile && destinationZone == Zone.EXILE) {
            newState = scheduleReboundRecast(
                newState, spellId, spellComponent.casterId, cardComponent?.name ?: "Unknown"
            )
        }

        // CR 715.3d — an Adventure card exiled by its own resolution may be cast as the creature
        // by the spell's controller while it remains in exile. Re-add the permission after
        // the prior removeMayPlayPermissionsForCard so the cast-from-exile enumerator picks
        // it up on the next priority pass.
        if (adventureFaceExile && destinationZone == Zone.EXILE) {
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm.addMayPlayPermission(
                com.wingedsheep.engine.state.permissions.MayPlayPermission(
                    id = permId,
                    cardIds = setOf(spellId),
                    controllerId = spellComponent.casterId,
                    permanent = true,
                    primaryFaceOnly = true,
                    timestamp = state.timestamp,
                )
            )
        }

        // Omen (Tarkir: Dragonstorm), and the Zenith cycle's printed "Shuffle <name> into its
        // owner's library.": the card was just added to the bottom of its owner's library above —
        // now shuffle that library and announce it. Gated on the *final* destination so a
        // RedirectZoneChange that sent the card elsewhere doesn't shuffle for nothing, and so
        // AfterResolveDestination.BOTTOM_OF_LIBRARY (which also lands in Zone.LIBRARY, but must
        // not shuffle) is left alone.
        if ((omenFaceShuffle || selfShuffleIntoLibrary) && destinationZone == Zone.LIBRARY) {
            newState = SpellZoneMoves.shuffleOwnerLibrary(newState, ownerId)
            events.add(LibraryShuffledEvent(ownerId))
        }
        return newState
    }

    /**
     * The riders a resolution-destination component (Goliath Daydreamer, Lilah) and a
     * `RedirectZoneChange` replacement (Valgavoth) attach to the moved card.
     */
    private fun applyAfterResolveAndRedirectRiders(
        current: GameState,
        spellId: EntityId,
        cardComponent: CardComponent?,
        ownerId: EntityId,
        destinationZone: Zone,
        destination: SpellDestination,
        redirect: com.wingedsheep.engine.handlers.effects.ZoneChangeRedirectResult,
        events: MutableList<GameEvent>
    ): GameState {
        var newState = current
        val exileAfterResolveComp = destination.exileAfterResolveComp
        // Add counters granted by AfterResolveDestinationComponent (e.g., Goliath Daydreamer's dream counter).
        if (destinationZone == Zone.EXILE && exileAfterResolveComp != null && exileAfterResolveComp.withCounters.isNotEmpty()) {
            newState = applyExileCounters(newState, spellId, exileAfterResolveComp.withCounters, events)
        }

        // "Exile that spell instead of putting it into your graveyard as it resolves. If you do, …"
        // (MarkSpellExileOnResolveEffect): the follow-up applies only once the card is in exile.
        if (destinationZone == Zone.EXILE && destination.exiledRiderFollows) {
            when (exileAfterResolveComp?.exiledRider) {
                // Lilah, Undefeated Slickshot: "If you do, it becomes plotted."
                com.wingedsheep.sdk.scripting.effects.ExiledSpellRider.BECOMES_PLOTTED ->
                    newState = SpellZoneMoves.applyPlottedToExiledCard(
                        newState, spellId, ownerId, cardComponent?.name ?: "Unknown", events
                    )
                // Feather, the Redeemed: "If you do, return it to your hand at the beginning of the
                // next end step."
                com.wingedsheep.sdk.scripting.effects.ExiledSpellRider.RETURN_TO_HAND_AT_NEXT_END_STEP ->
                    newState = scheduleEndStepReturnToHand(
                        newState, spellId, exileAfterResolveComp.exiledRiderControllerId ?: ownerId,
                        cardComponent?.name ?: "Unknown"
                    )
                null -> Unit
            }
        }

        // Link the exiled spell back to the source permanent (Goliath Daydreamer)
        // so the UI can display it tethered under the source and so the attack-trigger
        // free-cast ability can find it via the linked-exile pile.
        if (destinationZone == Zone.EXILE && exileAfterResolveComp?.linkedSourceId != null) {
            val sourceId = exileAfterResolveComp.linkedSourceId
            if (newState.getEntity(sourceId) != null) {
                newState = newState.updateEntity(sourceId) { c ->
                    val existing = c.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                    val updated = (existing?.exiledIds ?: emptyList()) + spellId
                    c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(updated))
                }
            }
        }

        // Link an opponent's resolving spell exiled by a RedirectZoneChange(linkToSource)
        // replacement (Valgavoth, Terror Eater) so its controller may later play it.
        if (destinationZone == Zone.EXILE && redirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, redirect.linkSourceId)
        }

        redirect.additionalEffect?.let { extra ->
            val (updatedState, extraEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                zones,
                newState, extra, redirect.effectControllerId, spellId,
                sourceId = redirect.effectSourceId,
                riderTiming = redirect.riderTiming
            )
            newState = updatedState
            events.addAll(extraEvents)
        }
        return newState
    }

    /**
     * Cipher (CR 702.99a): a resolving cipher spell represented by a card runs
     * [com.wingedsheep.sdk.scripting.Cipher.encodeStep] just after its other effects. A copy of a
     * cipher spell has the keyword too, but isn't represented by a card, so it encodes nothing.
     */
    private fun withCipherEncodeStep(
        state: GameState,
        spellId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        spellEffect: Effect
    ): Effect {
        if (cardDef?.keywords?.contains(com.wingedsheep.sdk.core.Keyword.CIPHER) != true) return spellEffect
        if (state.getEntity(spellId)?.has<CopyOfComponent>() == true) return spellEffect
        return com.wingedsheep.sdk.scripting.effects.CompositeEffect(
            listOf(spellEffect, com.wingedsheep.sdk.scripting.Cipher.encodeStep)
        )
    }

    /**
     * Encode the just-exiled cipher card on [creatureId] (CR 702.99b): mark the card with the
     * creature and its own exile object, and grant the creature the cipher trigger for as long as
     * that holds (CR 702.99c). The grant carries a fresh timestamp — it is the effect of the card's
     * static ability, which starts now — so a "loses all abilities" effect from later strips it and
     * one from earlier doesn't (CR 613.7). Nothing is encoded if the creature has left the
     * battlefield in the meantime.
     */
    private fun encodeOnCreature(
        state: GameState,
        cardId: EntityId,
        creatureId: EntityId,
        cardName: String
    ): GameState {
        if (creatureId !in state.getBattlefield()) return state
        val cardObject = state.objectRef(cardId) ?: return state
        // Stamp with the current timestamp, then tick, so an effect created later sorts after
        // this one (the LayerTimestamps convention).
        return state
            .updateEntity(cardId) {
                it.with(
                    com.wingedsheep.engine.state.components.battlefield.EncodedOnComponent(creatureId, cardObject)
                )
            }
            .copy(
                grantedTriggeredAbilities = state.grantedTriggeredAbilities +
                    com.wingedsheep.engine.event.GrantedTriggeredAbility(
                        entityId = creatureId,
                        ability = com.wingedsheep.sdk.scripting.Cipher.encodedCardTrigger(cardId, cardName),
                        duration = com.wingedsheep.sdk.scripting.Duration.WhileSourceEncodedOnAffected,
                        sourceId = cardId,
                        timestamp = state.timestamp
                    )
            )
            .tick()
    }

    /**
     * Rebound (CR 702.88): a spell has rebound if the printed keyword is on its card definition
     * or the keyword was granted to this stack object (Ojer Pakpatiq via GrantKeywordToSpellEffect,
     * stored on [SpellGrantedKeywordsComponent]). Only matters for a spell cast from hand — the
     * caller gates on [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent.castFromZone].
     */
    private fun spellHasRebound(
        state: GameState,
        spellId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Boolean {
        if (cardDef?.keywords?.contains(com.wingedsheep.sdk.core.Keyword.REBOUND) == true) return true
        val granted = state.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent>()
        return granted?.keywords?.contains(com.wingedsheep.sdk.core.Keyword.REBOUND.name) == true
    }

    /**
     * Schedule rebound's delayed triggered ability (CR 702.88a): at the beginning of the caster's
     * next upkeep, they may cast the just-exiled card from exile without paying its mana cost.
     * A one-shot step-based delayed trigger gated to the caster's turn ([fireOnPlayerId]) and to a
     * later turn ([notBeforeTurn]); it is consumed the first time it fires. The free cast reuses the
     * suspend/Shiko cast-from-exile pipeline ([CastFromCollectionWithoutPayingCostEffect]).
     */
    private fun scheduleReboundRecast(
        state: GameState,
        exiledCardId: EntityId,
        casterId: EntityId,
        sourceName: String
    ): GameState {
        val (triggerId, allocatedState) = state.newRoutingId()
        return allocatedState.addDelayedTrigger(
            com.wingedsheep.engine.event.DelayedTriggeredAbility(
                id = triggerId,
                effect = com.wingedsheep.sdk.dsl.Effects.May(
                    com.wingedsheep.sdk.scripting.effects.CompositeEffect(
                        listOf(
                            com.wingedsheep.sdk.scripting.effects.GatherCardsEffect(
                                source = com.wingedsheep.sdk.scripting.effects.CardSource.Entity(com.wingedsheep.sdk.scripting.targets.EffectTarget.Self),
                                storeAs = "rebound_recast",
                            ),
                            com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect(
                                from = "rebound_recast",
                            ),
                        )
                    ),
                    descriptionOverride = "cast this card from exile without paying its mana cost",
                ),
                fireAtStep = com.wingedsheep.sdk.core.Step.UPKEEP,
                fireOnPlayerId = casterId,
                notBeforeTurn = state.turnNumber + 1,
                sourceId = exiledCardId,
                objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                    captured = true,
                    origin = state.objectRef(exiledCardId),
                    source = state.objectRef(exiledCardId),
                ),
                sourceName = sourceName,
                controllerId = casterId,
            )
        )
    }

    /**
     * Schedule [com.wingedsheep.sdk.scripting.effects.ExiledSpellRider.RETURN_TO_HAND_AT_NEXT_END_STEP]'s
     * delayed triggered ability: at the beginning of the next end step (this turn's, unless this
     * turn's end step has already begun), return the just-exiled card to its owner's hand. The trigger captures the
     * card's exile object, so if the card leaves exile before then it is a new object (CR 400.7) and
     * stays where it is. It is consumed the first time it fires.
     */
    private fun scheduleEndStepReturnToHand(
        state: GameState,
        exiledCardId: EntityId,
        controllerId: EntityId,
        sourceName: String
    ): GameState {
        val (triggerId, allocatedState) = state.newRoutingId()
        return allocatedState.addDelayedTrigger(
            com.wingedsheep.engine.event.DelayedTriggeredAbility(
                id = triggerId,
                effect = com.wingedsheep.sdk.dsl.Effects.Move(
                    com.wingedsheep.sdk.scripting.targets.EffectTarget.Self, Zone.HAND, fromZone = Zone.EXILE
                ),
                fireAtStep = com.wingedsheep.sdk.core.Step.END,
                // Resolved during an end step (or cleanup): this turn's end step has already
                // begun, so "the next end step" is next turn's.
                notBeforeTurn = if (state.step == com.wingedsheep.sdk.core.Step.END ||
                    state.step == com.wingedsheep.sdk.core.Step.CLEANUP
                ) state.turnNumber + 1 else null,
                sourceId = exiledCardId,
                objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                    captured = true,
                    origin = state.objectRef(exiledCardId),
                    source = state.objectRef(exiledCardId),
                ),
                sourceName = sourceName,
                controllerId = controllerId,
            )
        )
    }

    /**
     * Resolution destination for [com.wingedsheep.sdk.model.CardScript.returnTransformedFromGraveyardOnResolve]
     * (Esper Origins): a spell cast from a graveyard is put onto the battlefield **transformed**
     * (its back face up) under its owner's control, entering with [counters], instead of going to
     * the graveyard/exile.
     *
     * Faithful to "exile it, then put it onto the battlefield transformed ... with a finality counter":
     * the resolved card leaves the stack and a brand-new back-face object enters the battlefield
     * (leaves/enters triggers fire, a Saga back enters with a fresh lore counter). The intermediate
     * exile is invisible — no effect keys on it — so the stack → battlefield move is done directly.
     *
     * Per the official ruling, a card that is not double-faced (or whose back face is not a permanent)
     * "will not enter at all"; [returnDfcFace] no-ops in that case and the caller must fall
     * back to the normal graveyard/exile destination.
     */
    private fun resolveSelfToBattlefieldTransformed(
        state: GameState,
        spellId: EntityId,
        counters: List<CounterType>,
        events: MutableList<GameEvent>
    ): GameState? {
        val container = state.getEntity(spellId) ?: return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val ownerId = cardComponent.ownerId ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.name) ?: return null
        val backFace = cardDef.backFace ?: return null
        // A non-permanent back face can't be put onto the battlefield — no-op, caller falls back.
        if (!backFace.isPermanent) return null

        // Strip the on-stack bookkeeping (and any alternative-cost permissions) before the card
        // becomes a permanent, mirroring the normal resolved-spell cleanup.
        var working = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TextReplacementComponent>()
                .without<TargetsComponent>()
                .without<PlayWithoutPayingCostComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostReductionComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent>()
                .without<AfterResolveDestinationComponent>()
        }
        working = working.removeMayPlayPermissionsForCard(spellId)

        // "Exile it, then put it onto the battlefield transformed": the resolving spell was already
        // popped off the stack (it is in no zone), so place it in its owner's exile — the source
        // zone [returnDfcFace] is built to flip-and-return from.
        working = working.addToZone(ZoneKey(ownerId, Zone.EXILE), spellId)

        // A DFC spell on the stack carries no DoubleFacedComponent yet (it's stamped on ETB); add
        // one on its front face so returnDfcFace can flip it to the back face.
        if (working.getEntity(spellId)?.get<DoubleFacedComponent>() == null) {
            working = working.updateEntity(spellId) { c ->
                c.with(
                    DoubleFacedComponent(
                        frontCardDefinitionId = cardDef.name,
                        backCardDefinitionId = backFace.name,
                        currentFace = DoubleFacedComponent.Face.FRONT
                    )
                )
            }
        }

        val transition = returnDfcFace(zones, working, cardRegistry, spellId, DoubleFacedComponent.Face.BACK)
        working = transition.state
        events.addAll(transition.events)

        // The finality counter (and any others) land on the new back-face permanent.
        if (counters.isNotEmpty()) {
            working = applyExileCounters(working, spellId, counters, events)
        }
        return working
    }

    /**
     * Add counters to a card that was just exiled because of AfterResolveDestinationComponent.
     * Used by Goliath Daydreamer to put a dream counter on cast spells as they're exiled.
     */
    private fun applyExileCounters(
        state: GameState,
        cardId: EntityId,
        counters: List<com.wingedsheep.sdk.core.CounterType>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: ""
        var updated = state
        for (counterType in counters) {
            updated = updated.updateEntity(cardId) { c ->
                val current = c.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                    ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()
                c.with(current.withAdded(counterType, 1))
            }
            events.add(CountersAddedEvent(cardId, counterType, 1, cardName))
        }
        return updated
    }
}

/**
 * The effect the spell resolves with: the cast face's (Adventure / split, CR 715 / 709), the
 * kicked or cleaved variant, or the card's own — with any text-changing effect applied. Shared
 * with [com.wingedsheep.sdk.scripting.predicates.CardPredicate.WouldDestroy], which previews it.
 */
internal fun resolvingSpellEffect(
    state: GameState,
    spellId: EntityId,
    spellComponent: SpellOnStackComponent,
    cardComponent: CardComponent?,
    resolvedCardDef: com.wingedsheep.sdk.model.CardDefinition?
): Effect? {
    // Execute the spell effect if present, applying text replacement if the spell
    // was modified by a text-changing effect (e.g., Artificial Evolution)
    // Use kickerSpellEffect when the spell was kicked and an alternate effect is defined.
    // Adventure / split face cast (CR 715 / 709) — when the spell was cast as a face, read
    // the face's spell effect from `cardDef.cardFaces[faceIndex].script.spellEffect`.
    val faceSpellEffect = spellComponent.faceIndex?.let { idx ->
        resolvedCardDef?.cardFaces?.getOrNull(idx)?.script?.spellEffect
    }
    val baseSpellEffect = when {
        faceSpellEffect != null -> faceSpellEffect
        spellComponent.declaredCostSlot != null && cardComponent != null ->
            resolvedCardDef?.script?.kickerSpellEffect ?: cardComponent.spellEffect
        // Cleave (CR 702.148): a spell cast for its cleave cost resolves with its
        // brackets-removed effect variant, applied structurally at cast time rather than by
        // editing text — so e.g. a bracketed delayed-trigger clause is never created.
        spellComponent.wasCleaved && cardComponent != null ->
            resolvedCardDef?.script?.cleaveSpellEffect ?: cardComponent.spellEffect
        // Overload (CR 702.96a): "target" reads "each" — the author-written untargeted variant.
        spellComponent.wasOverloaded && cardComponent != null ->
            resolvedCardDef?.script?.overloadSpellEffect ?: cardComponent.spellEffect
        else -> cardComponent?.spellEffect
    }
    val rawSpellEffect = baseSpellEffect
    val textReplacement = TextChanges.forSpell(state, spellId)
    return if (rawSpellEffect != null && textReplacement != null) {
        rawSpellEffect.applyTextReplacement(textReplacement)
    } else {
        rawSpellEffect
    }
}
