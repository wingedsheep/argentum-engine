package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.handlers.effects.library.CascadeExecutor
import com.wingedsheep.engine.handlers.effects.library.ChooseOnePerCategoryExecutor
import com.wingedsheep.engine.handlers.effects.library.CastAnyNumberFromCollectionWithoutPayingCostExecutor
import com.wingedsheep.engine.handlers.effects.library.CastFromCollectionWithoutPayingCostExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

class LibraryAndZoneContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val targetFinder: TargetFinder
) : ContinuationResumerModule {

    private val castSpellHandler: CastSpellHandler get() = services.castSpellHandler
    private val effectRunner: EffectContinuationRunner by lazy {
        EffectContinuationRunner(services.effectExecutorRegistry)
    }

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ReturnFromGraveyardContinuation::class, ::resumeReturnFromGraveyard),
        resumer(MoveCollectionOrderContinuation::class, ::resumeMoveCollectionOrder),
        resumer(PutOnBottomOfLibraryContinuation::class, ::resumePutOnBottomOfLibrary),
        resumer(PutFromHandContinuation::class, ::resumePutFromHand),
        resumer(SelectFromCollectionContinuation::class, ::resumeSelectFromCollection),
        resumer(ChooseOnePerCategoryContinuation::class, ::resumeChooseOnePerCategory),
        resumer(ChoosePileContinuation::class, ::resumeChoosePile),
        resumer(SelectTargetPipelineContinuation::class, ::resumeSelectTargetPipeline),
        resumer(MoveCollectionAuraTargetContinuation::class, ::resumeMoveCollectionAuraTarget),
        resumer(PutOntoBattlefieldAttachedToChosenContinuation::class, ::resumePutOntoBattlefieldAttachedToChosen),
        resumer(AttachToChosenHostContinuation::class, ::resumeAttachToChosenHost),
        resumer(PutOnTopOrBottomContinuation::class, ::resumePutOnTopOrBottom),
        resumer(CascadeMayCastContinuation::class, ::resumeCascadeMayCast),
        resumer(DiscoverMayCastContinuation::class, ::resumeDiscoverMayCast),
        resumer(CastFromCollectionTargetsContinuation::class, ::resumeCastFromCollectionTargets),
        resumer(CastAnyNumberFromCollectionContinuation::class, ::resumeCastAnyNumberFromCollection)
    )

    fun resumeReturnFromGraveyard(
        state: GameState,
        continuation: ReturnFromGraveyardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for graveyard search")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Empty selection — no card returned
        if (selectedCards.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val cardId = selectedCards.first()
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        // Validate card is still in graveyard
        if (cardId !in state.getZone(graveyardZone)) {
            return checkForMore(state, emptyList())
        }

        val destZone = when (continuation.destination) {
            SearchDestination.HAND -> Zone.HAND
            SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD
            else -> return ExecutionResult.error(state, "Unsupported destination: ${continuation.destination}")
        }

        // Delegate zone movement to ZoneTransitionService for full cleanup + entry setup
        val transitionResult = services.zones.moveToZone(
            state, cardId, destZone,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(controllerId = playerId),
            ZoneKey(playerId, Zone.GRAVEYARD)
        )

        return checkForMore(transitionResult.state, transitionResult.events)
    }

    /**
     * Resume after player ordered cards for a MoveCollection with ControllerChooses order.
     *
     * The response contains the card IDs in the new order (first = new top of library).
     * We remove the cards from their current zones and place them on top in the chosen order.
     */
    fun resumeMoveCollectionOrder(
        state: GameState,
        continuation: MoveCollectionOrderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for MoveCollection order")
        }

        val orderedCards = response.orderedObjects
        val destPlayerId = continuation.destinationPlayerId
        val libraryZone = ZoneKey(destPlayerId, Zone.LIBRARY)

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Source storage can be any owner's zone or the stack. A stolen permanent's
        // battlefield bucket is not necessarily its owner's bucket.
        for (cardId in orderedCards) {
            val currentZone = newState.zones.entries.firstOrNull { cardId in it.value }?.key
            if (currentZone != null) newState = newState.removeFromZone(currentZone, cardId)
            if (cardId in newState.stack) newState = newState.removeFromStack(cardId)
        }

        // Positional entry distinguishes real arrivals from same-library ordering.
        val insertionIndex = if (continuation.placement == ZonePlacement.Bottom)
            newState.getZone(libraryZone).size else 0
        for ((offset, cardId) in orderedCards.withIndex()) {
            val oldObject = newState.objectRef(cardId)
            val origin = newState.logicalZone(cardId)
            newState = newState.insertIntoZone(libraryZone, cardId, insertionIndex + offset)
            if (origin != null && origin != libraryZone) {
                events.add(ZoneChangeEvent(
                    entityId = cardId,
                    entityName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown",
                    fromZone = origin.zoneType,
                    toZone = Zone.LIBRARY,
                    ownerId = newState.getEntity(cardId)?.get<CardComponent>()?.ownerId ?: destPlayerId,
                    oldObject = oldObject,
                    newObject = newState.objectRef(cardId)
                ))
            }
        }

        events.add(
            LibraryReorderedEvent(
                playerId = continuation.playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after player ordered cards to put on the bottom of their library.
     *
     * Same as resumeReorderLibrary but places cards on the BOTTOM of the library
     * instead of the top. Used for effects like Erratic Explosion.
     */
    fun resumePutOnBottomOfLibrary(
        state: GameState,
        continuation: PutOnBottomOfLibraryContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for library bottom reorder")
        }

        val playerId = continuation.playerId
        val orderedCards = response.orderedObjects
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)

        // These are the same library objects, possibly detached by an older saved continuation.
        // Reinsert through the shared entry helper so the retained logical visit is preserved.
        var newState = state
        for (cardId in orderedCards) newState = newState.removeFromZone(libraryZone, cardId)
        for (cardId in orderedCards) newState = newState.addToZone(libraryZone, cardId)

        val events = listOf(
            LibraryReorderedEvent(
                playerId = playerId,
                cardCount = orderedCards.size,
                source = continuation.sourceName
            )
        )

        return checkForMore(newState, events)
    }

    fun resumePutFromHand(
        state: GameState,
        continuation: PutFromHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for put-from-hand")
        }

        // Player selected 0 cards — declined
        if (response.selectedCards.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val cardId = response.selectedCards.first()
        val playerId = continuation.playerId
        val handZone = ZoneKey(playerId, Zone.HAND)

        // Verify card is still in hand
        if (cardId !in state.getZone(handZone)) {
            return checkForMore(state, emptyList())
        }

        // Delegate zone movement to ZoneTransitionService for full entry setup (including Saga entry)
        val transitionResult = services.zones.moveToZone(
            state, cardId, Zone.BATTLEFIELD,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(
                controllerId = playerId,
                tapped = continuation.entersTapped
            ),
            ZoneKey(playerId, Zone.HAND)
        )

        return checkForMore(transitionResult.state, transitionResult.events)
    }

    /**
     * Resume after a player chose a target for an Aura entering via MoveCollectionEffect.
     * Moves the aura from current zone to battlefield with AttachedToComponent.
     */
    fun resumeMoveCollectionAuraTarget(
        state: GameState,
        continuation: MoveCollectionAuraTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for aura target selection")
        }

        val targetIds = response.selectedTargets[0] ?: emptyList()
        if (targetIds.isEmpty()) {
            return ExecutionResult.error(state, "No target selected for aura")
        }

        val targetId = targetIds.first()
        if (targetId in continuation.excludedHosts) {
            return ExecutionResult.error(state, "An Aura can't enchant an object entering the battlefield with it")
        }
        val auraId = continuation.auraId
        val destPlayerId = continuation.destPlayerId

        // Use MoveCollectionExecutor's helper to move aura to battlefield with attachment
        val executor = com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor(
            services.zones,
            cardRegistry = services.cardRegistry,
            targetFinder = services.targetFinder
        )
        val (newState, moveEvents) = executor.moveAuraToBattlefield(state, auraId, targetId, destPlayerId)

        // Continue with remaining auras
        val remainingAuras = continuation.remainingAuras
        if (remainingAuras.isNotEmpty()) {
            val nextAuraId = remainingAuras.first()
            val nextRemaining = remainingAuras.drop(1)

            // When underOwnersControl, use the next aura's owner as its controller
            val nextControllerId = if (continuation.underOwnersControl) {
                val e = newState.getEntity(nextAuraId)
                e?.get<OwnerComponent>()?.playerId
                    ?: e?.get<CardComponent>()?.ownerId
                    ?: continuation.controllerId
            } else continuation.controllerId

            val nextCardComponent = newState.getEntity(nextAuraId)?.get<CardComponent>()
            val nextCardDef = nextCardComponent?.let { services.cardRegistry.getCard(it.cardDefinitionId) }
            val nextAuraTarget = nextCardDef?.script?.auraTarget

            if (nextAuraTarget == null) {
                // Skip this aura, continue to next
                return resumeMoveCollectionAuraTarget(
                    newState,
                    continuation.copy(
                        auraId = nextAuraId,
                        controllerId = nextControllerId,
                        destPlayerId = nextControllerId,
                        remainingAuras = nextRemaining,
                    ),
                    response,
                    checkForMore
                )
            }

            val legalTargets = services.targetFinder.findLegalTargets(
                state = newState,
                requirement = nextAuraTarget,
                controllerId = nextControllerId,
                sourceId = nextAuraId,
                ignoreTargetingRestrictions = true
            ).filter { it !in continuation.excludedHosts }

            if (legalTargets.isEmpty()) {
                // No targets — Aura stays in current zone (Rule 303.4g), continue to next
                if (nextRemaining.isNotEmpty()) {
                    return resumeMoveCollectionAuraTarget(
                        newState,
                        continuation.copy(
                            auraId = nextRemaining.first(),
                            controllerId = nextControllerId,
                            destPlayerId = nextControllerId,
                            remainingAuras = nextRemaining.drop(1),
                        ),
                        response,
                        checkForMore
                    )
                }
                return checkForMore(newState, moveEvents)
            }

            // Pause for next aura target
            val auraName = nextCardComponent.name
            val requirementInfo = TargetRequirementInfo(
                index = 0,
                description = nextAuraTarget.description,
                minTargets = 1,
                maxTargets = 1
            )
            val question = { decisionId: String -> ChooseTargetsDecision(
                id = decisionId,
                playerId = nextControllerId,
                prompt = "Choose what $auraName enchants",
                context = DecisionContext(
                    sourceId = nextAuraId,
                    sourceName = auraName,
                    phase = DecisionPhase.RESOLUTION
                ),
                targetRequirements = listOf(requirementInfo),
                legalTargets = mapOf(0 to legalTargets)
            ) }

            val nextContinuation = MoveCollectionAuraTargetContinuation(
                auraId = nextAuraId,
                controllerId = nextControllerId,
                destPlayerId = nextControllerId,
                remainingAuras = nextRemaining,
                sourceId = continuation.sourceId,
                objectReferences = continuation.objectReferences,
                sourceName = continuation.sourceName,
                underOwnersControl = continuation.underOwnersControl,
                excludedHosts = continuation.excludedHosts
            )

            return newState.suspendForDecision(question, nextContinuation, moveEvents)
        }

        return checkForMore(newState, moveEvents)
    }

    /**
     * Resume after the controller chooses a host for a card put onto the battlefield attached to
     * a chosen permanent (One Last Job mode 3). Moves the Aura/Equipment to the battlefield under
     * the controller's control and attaches it to the chosen host, reusing the permanent-agnostic
     * [com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor.moveAuraToBattlefield].
     */
    fun resumePutOntoBattlefieldAttachedToChosen(
        state: GameState,
        continuation: PutOntoBattlefieldAttachedToChosenContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for attach-host selection")
        }

        val hostIds = response.selectedTargets[0] ?: emptyList()
        if (hostIds.isEmpty()) {
            // No host chosen — leave the card where it is (mode does nothing).
            return checkForMore(state, emptyList())
        }
        val hostId = hostIds.first()

        // Host must still be on the battlefield.
        if (!state.getBattlefield().contains(hostId)) {
            return checkForMore(state, emptyList())
        }

        val executor = com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor(
            services.zones,
            cardRegistry = services.cardRegistry,
            targetFinder = services.targetFinder
        )
        val (newState, events) = executor.moveAuraToBattlefield(
            state, continuation.cardId, hostId, continuation.controllerId
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after the controller chooses the new host for an Aura/Equipment already on the
     * battlefield (AttachToChosenHostEffect). Re-checks legality, then moves it.
     */
    fun resumeAttachToChosenHost(
        state: GameState,
        continuation: AttachToChosenHostContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for attach-host selection")
        }
        val hostId = response.selectedTargets[0]?.firstOrNull()
            ?: return checkForMore(state, emptyList())
        if (!AttachmentMover.canAttach(
                state, services.predicateEvaluator, services.cardRegistry, continuation.attachmentId, hostId
            )
        ) {
            return checkForMore(state, emptyList())
        }
        val (newState, events) = AttachmentMover.attach(state, continuation.attachmentId, hostId, continuation.controllerId)
        return checkForMore(newState, events)
    }

    private fun resumeSpellSelection(
        state: GameState,
        continuation: SelectFromCollectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val selected = continuation.selectedSpellCard ?: (response as? CardsSelectedResponse)?.selectedCards?.singleOrNull()
        if (selected == null) {
            if (response !is CardsSelectedResponse || response.selectedCards.isNotEmpty())
                return ExecutionResult.error(state, "Choose at most one spell")
            val collections = mutableMapOf(continuation.storeSelected to emptyList<EntityId>())
            continuation.storeRemainder?.let { collections[it] = continuation.allCards }
            return checkForMore(exposeCollectionsToNextFrame(state, collections), emptyList())
        }
        val faces = continuation.spellFaces?.get(selected)
            ?: return ExecutionResult.error(state, "That card has no matching spell face")
        val chosenFace = if (continuation.selectedSpellCard != null) {
            val index = (response as? OptionChosenResponse)?.optionIndex
                ?: return ExecutionResult.error(state, "Expected a spell face choice")
            faces.getOrNull(index) ?: return ExecutionResult.error(state, "Invalid spell face")
        } else if (faces.size == 1) faces.single() else {
            val card = state.getEntity(selected)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?: return ExecutionResult.error(state, "Selected card is missing")
            val definition = services.cardRegistry.getCard(card.cardDefinitionId)
            return state.suspendForDecision(
                { id -> ChooseOptionDecision(
                    id = id, playerId = continuation.playerId, prompt = "Choose which spell to cast",
                    context = DecisionContext(sourceId = continuation.sourceId, sourceName = continuation.sourceName, phase = DecisionPhase.RESOLUTION),
                    options = faces.map {
                        when (it) {
                            -1 -> card.name
                            -2 -> definition!!.backFace!!.name
                            else -> definition!!.cardFaces[it].name
                        }
                    },
                    optionCardIds = faces.indices.associateWith { listOf(selected) }
                ) },
                continuation.copy(selectedSpellCard = selected)
            )
        }
        val collections = mutableMapOf(continuation.storeSelected to listOf(selected))
        continuation.storeRemainder?.let { collections[it] = continuation.allCards - selected }
        return checkForMore(exposeCollectionsToNextFrame(
            state, collections,
            numbers = mapOf(com.wingedsheep.engine.handlers.PipelineState.spellFaceKey(continuation.storeSelected) to chosenFace)
        ), emptyList())
    }

    fun resumeSelectFromCollection(
        state: GameState,
        continuation: SelectFromCollectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (continuation.spellFaces != null) {
            return resumeSpellSelection(state, continuation, response, checkForMore)
        }
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for SelectFromCollection")
        }

        // Apply any selection restrictions server-side. Iterate the player's
        // response order so earlier picks win when a restriction rejects a later
        // one; rejected cards fall through into the remainder collection.
        val acceptedSet: Set<EntityId> = if (continuation.restrictions.isEmpty()) {
            response.selectedCards.toSet()
        } else {
            val kept = mutableSetOf<EntityId>()
            val claimedTypes = mutableSetOf<com.wingedsheep.sdk.core.CardType>()
            val claimedColors = mutableSetOf<com.wingedsheep.sdk.core.Color>()
            val claimedNames = mutableSetOf<String>()
            val claimedLandTypes = mutableSetOf<com.wingedsheep.sdk.core.Subtype>()
            val claimedPowers = mutableSetOf<Int>()
            var runningManaValue = 0
            var runningPower = 0
            // A card's projected power (after continuous effects), or 0 if undefined. Used by
            // TotalPowerAtMost — battlefield P/T must read projection (CLAUDE.md).
            fun projectedPowerOf(cardId: EntityId): Int =
                state.projectedState.getPower(cardId) ?: 0
            // A card's fixed (printed) power, or null for cards with no fixed power.
            fun fixedPowerOf(cardId: EntityId): Int? =
                state.getEntity(cardId)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                    ?.baseStats?.basePower
            // Basic land subtypes a card has (Plains/Island/Swamp/Mountain/Forest), for OnePerBasicLandType.
            fun basicLandTypesOf(cardId: EntityId): Set<com.wingedsheep.sdk.core.Subtype> =
                state.getEntity(cardId)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                    ?.typeLine?.subtypes
                    ?.filter { it.value in com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES }
                    ?.toSet() ?: emptySet()
            for (cardId in response.selectedCards) {
                val acceptsAllRestrictions = continuation.restrictions.all { restriction ->
                    when (restriction) {
                        is SelectionRestriction.OnePerCardType -> {
                            val cardTypes = state.getEntity(cardId)
                                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                ?.typeLine?.cardTypes ?: emptySet()
                            cardTypes.isEmpty() || cardTypes.none { it in claimedTypes }
                        }
                        is SelectionRestriction.OnePerColor -> {
                            val cardColors = state.getEntity(cardId)
                                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                ?.colors ?: emptySet()
                            // Colourless cards are not constrained by this restriction.
                            cardColors.isEmpty() || cardColors.none { it in claimedColors }
                        }
                        is SelectionRestriction.OnePerCardName -> {
                            val cardName = state.getEntity(cardId)
                                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                ?.name
                            cardName == null || cardName !in claimedNames
                        }
                        is SelectionRestriction.TotalManaValueAtMost -> {
                            val mv = state.getEntity(cardId)
                                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                ?.manaValue ?: 0
                            runningManaValue + mv <= restriction.max
                        }
                        is SelectionRestriction.TotalPowerAtMost -> {
                            runningPower + projectedPowerOf(cardId) <= restriction.max
                        }
                        is SelectionRestriction.OnePerBasicLandType -> {
                            val types = basicLandTypesOf(cardId)
                            // A typeless land can't be kept; a typed land needs all its types free.
                            types.isNotEmpty() && types.none { it in claimedLandTypes }
                        }
                        is SelectionRestriction.OnePerPower -> {
                            // A card with no fixed power can't be kept; otherwise its power must be free.
                            val power = fixedPowerOf(cardId)
                            power != null && power !in claimedPowers
                        }
                        is SelectionRestriction.ReducedMinimumIfMatches -> true
                        is SelectionRestriction.MaxAffordablePayment ->
                            // A pure count cap, already folded into the decision's maxSelections
                            // at decision-build time and enforced by response validation; game
                            // state can't change while the decision is pending, so there is
                            // nothing to re-check per card here.
                            true
                    }
                }
                if (acceptsAllRestrictions) {
                    kept += cardId
                    // Update restriction bookkeeping for subsequent picks.
                    for (restriction in continuation.restrictions) {
                        when (restriction) {
                            is SelectionRestriction.OnePerCardType -> {
                                claimedTypes += state.getEntity(cardId)
                                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                    ?.typeLine?.cardTypes ?: emptySet()
                            }
                            is SelectionRestriction.OnePerColor -> {
                                claimedColors += state.getEntity(cardId)
                                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                    ?.colors ?: emptySet()
                            }
                            is SelectionRestriction.OnePerCardName -> {
                                val cardName = state.getEntity(cardId)
                                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                    ?.name
                                if (cardName != null) claimedNames += cardName
                            }
                            is SelectionRestriction.TotalManaValueAtMost -> {
                                runningManaValue += state.getEntity(cardId)
                                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                    ?.manaValue ?: 0
                            }
                            is SelectionRestriction.TotalPowerAtMost -> {
                                runningPower += projectedPowerOf(cardId)
                            }
                            is SelectionRestriction.OnePerBasicLandType -> {
                                claimedLandTypes += basicLandTypesOf(cardId)
                            }
                            is SelectionRestriction.OnePerPower -> {
                                fixedPowerOf(cardId)?.let { claimedPowers += it }
                            }
                            is SelectionRestriction.ReducedMinimumIfMatches -> {
                                // Response validation enforces the conditional minimum.
                            }
                            is SelectionRestriction.MaxAffordablePayment -> {
                                // Count cap — no per-card bookkeeping (see the accept check above).
                            }
                        }
                    }
                }
            }
            kept
        }

        val selected = continuation.allCards.filter { it in acceptedSet }
        val remainder = continuation.allCards.filter { it !in acceptedSet }

        // Build the updated collections
        val updatedCollections = continuation.storedCollections.toMutableMap()
        updatedCollections[continuation.storeSelected] = selected
        if (continuation.storeRemainder != null) {
            updatedCollections[continuation.storeRemainder] = remainder
        }

        // Inject updated collections into the consumer frame beneath (if any)
        val newState = exposeCollectionsToNextFrame(state, updatedCollections)

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after one chooser answered one category of a
     * [com.wingedsheep.sdk.scripting.effects.ChooseOnePerCategoryEffect] ("chooses a permanent they
     * control of each permanent type"): record the pick and re-enter the collect loop, which either
     * asks the next question or — once every chooser is done — publishes the picks so the
     * downstream "…the rest" steps can act on them.
     */
    fun resumeChooseOnePerCategory(
        state: GameState,
        continuation: ChooseOnePerCategoryContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for ChooseOnePerCategory")
        }

        val result = ChooseOnePerCategoryExecutor(predicateEvaluator = services.predicateEvaluator).collectPicks(
            state = state,
            effect = continuation.effect,
            storedCollections = continuation.storedCollections,
            pendingPlayers = continuation.pendingPlayers,
            startCategory = continuation.categoryIndex + 1,
            picks = continuation.picks + response.selectedCards,
            sourceId = continuation.sourceId,
            objectReferences = continuation.objectReferences
        )

        if (result.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(result.state, result.events)
        }

        // Republish the pipeline's collections alongside the picks so the consumer frame sees both
        // the original pool and the kept set.
        val merged = continuation.storedCollections + result.updatedCollections
        return checkForMore(exposeCollectionsToNextFrame(result.state, merged), result.events)
    }

    /**
     * Resume after the chooser picked one of two pre-existing piles via
     * [com.wingedsheep.sdk.scripting.effects.ChoosePileEffect]. Routes pile A
     * or pile B (per [OptionChosenResponse.optionIndex]) to [storeChosenAs],
     * and the other to [storeOtherAs], on the next [EffectContinuation].
     */
    fun resumeChoosePile(
        state: GameState,
        continuation: ChoosePileContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for ChoosePile")
        }
        val (chosen, other) = when (response.optionIndex) {
            0 -> continuation.pileAIds to continuation.pileBIds
            1 -> continuation.pileBIds to continuation.pileAIds
            else -> return ExecutionResult.error(
                state,
                "Invalid pile index for ChoosePile: ${response.optionIndex}"
            )
        }

        val updatedCollections = continuation.storedCollections.toMutableMap()
        updatedCollections[continuation.storeChosenAs] = chosen
        updatedCollections[continuation.storeOtherAs] = other

        val newState = exposeCollectionsToNextFrame(state, updatedCollections)

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after a player selected a target during a pipeline effect (SelectTargetEffect).
     *
     * Extracts the selected target IDs from the [TargetsResponse], stores them under
     * [SelectTargetPipelineContinuation.storeAs], and injects the updated collections
     * into the next [EffectContinuation] on the stack.
     */
    fun resumeSelectTargetPipeline(
        state: GameState,
        continuation: SelectTargetPipelineContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for SelectTargetPipeline")
        }

        val selectedTargetIds = response.selectedTargets[0] ?: emptyList()

        // Build the updated collections
        val updatedCollections = continuation.storedCollections.toMutableMap()
        updatedCollections[continuation.storeAs] = selectedTargetIds

        // Inject updated collections into the consumer frame beneath (if any)
        val newState = exposeCollectionsToNextFrame(state, updatedCollections)

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after a card's owner chose top or bottom of their library.
     * Moves the card to the chosen position via ZoneTransitionService, or — if the
     * target is a spell on the stack — removes the spell (it isn't countered) and places it directly
     * onto the chosen end of the owner's library.
     */
    fun resumePutOnTopOrBottom(
        state: GameState,
        continuation: PutOnTopOrBottomContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for top/bottom of library")
        }

        if (response.optionIndex !in continuation.options.indices) {
            return ExecutionResult.error(state, "Invalid option index: ${response.optionIndex}")
        }

        val chosenPosition = continuation.positions.getOrNull(response.optionIndex)
            ?: run {
                // Backwards-compatible fallback: continuations serialised before
                // `positions` was added carry only option strings.
                when (continuation.options[response.optionIndex]) {
                    "Top of library" -> com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Top
                    "Second from top of library" -> com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.SecondFromTop
                    "Bottom of library" -> com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Bottom
                    else -> return ExecutionResult.error(state, "Unknown library position option")
                }
            }

        val placement = when (chosenPosition) {
            com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Top ->
                com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top
            com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.SecondFromTop ->
                com.wingedsheep.engine.handlers.effects.LibraryPlacement.NthFromTop(1)
            com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Bottom ->
                com.wingedsheep.engine.handlers.effects.LibraryPlacement.Bottom
        }

        val cardId = continuation.cardId

        // Case 1: target is a spell on the stack — remove from stack and place in library.
        if (cardId in state.stack) {
            return resumePutSpellOnTopOrBottom(state, cardId, continuation.ownerId, placement, checkForMore)
        }

        // Case 2: target is in a zone (battlefield or elsewhere) — use ZoneTransitionService.
        val currentZone = state.zones.entries.firstOrNull { (_, entities) -> cardId in entities }?.key
            ?: return checkForMore(state, emptyList()) // Card no longer exists in any zone

        val transitionResult = services.zones.moveToZone(
            state, cardId, Zone.LIBRARY,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(
                controllerId = continuation.ownerId,
                libraryPlacement = placement
            ),
            currentZone
        )

        // The card was visible to everyone before the move (battlefield or stack) and the owner's
        // choice of position was public, so all players know where it ended up. Mark it revealed
        // to every player so each library viewer shows the card face-up at its new slot.
        val finalState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .markRevealed(transitionResult.state, listOf(cardId), transitionResult.state.turnOrder.toSet())

        return checkForMore(finalState, transitionResult.events)
    }

    /**
     * Handle the stack case for [PutOnTopOrBottomContinuation]: counter the spell
     * (remove from stack + strip stack components) and insert it into the owner's
     * library at the chosen end. Can't-be-countered spells still follow the effect
     * per the general MTG rules — putting a spell into its owner's library is not
     * countering it in the technical sense, so we always move it.
     */
    private fun resumePutSpellOnTopOrBottom(
        state: GameState,
        spellId: EntityId,
        ownerId: EntityId,
        placement: com.wingedsheep.engine.handlers.effects.LibraryPlacement,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val spellContainer = state.getEntity(spellId)
            ?: return checkForMore(state, emptyList())
        val spellName = spellContainer.get<CardComponent>()?.name ?: "Unknown"

        var newState = state.removeFromStack(spellId)
        newState = newState.updateEntity(spellId) { c ->
            c.without<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()
                .without<com.wingedsheep.engine.state.components.stack.TargetsComponent>()
        }

        val libZoneKey = ZoneKey(ownerId, Zone.LIBRARY)
        val currentLibrary = newState.getZone(libZoneKey)
        val insertIndex = when (placement) {
            com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top -> 0
            is com.wingedsheep.engine.handlers.effects.LibraryPlacement.NthFromTop -> placement.position
            else -> currentLibrary.size
        }
        newState = newState.insertIntoZone(libZoneKey, spellId, insertIndex)

        // Both players watched the spell get placed at this position — mark it revealed to all
        // so each side's library viewer shows it face-up at the new slot.
        newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .markRevealed(newState, listOf(spellId), newState.turnOrder.toSet())

        // Not a counter: "the owner of target spell puts it on … their library" (Sudden Setback,
        // Swat Away) moves the spell, so no SpellCounteredEvent — and Guile's counter replacement
        // (ExileCounteredSpellInstead) rightly never sees it.
        val events = listOf(
            ZoneChangeEvent(
                entityId = spellId,
                entityName = spellName,
                fromZone = Zone.STACK,
                toZone = Zone.LIBRARY,
                oldObject = state.objectRef(spellId),
                newObject = newState.objectRef(spellId),
                ownerId = ownerId
            )
        )
        return checkForMore(newState, events)
    }

    /**
     * Resume cascade resolution (CR 702.85a) after the controller answers
     * "cast this card without paying its mana cost?".
     *
     * On **No** every exiled card — including the would-be cascade card — is
     * shuffled onto the bottom of the controller's library.
     *
     * On **Yes** the other exiled cards (the lands and any other non-hit cards
     * skipped past during the walk) are bottomed first. The cascade card is
     * granted [MayPlayPermission] + [PlayWithoutPayingCostComponent] so the
     * synthesized cast resolves to a free cast, then [CastSpellHandler] is
     * invoked directly to put the spell on the stack. If the cast pauses for
     * targets / X / modes, that pause is bubbled up unchanged — the leftover
     * bottoming has already happened, so the cascade resolution is effectively
     * complete. If the cast errors (no legal targets, etc.) the cascade card
     * is bottomed too, since it ultimately wasn't cast.
     */
    fun resumeCascadeMayCast(
        state: GameState,
        continuation: CascadeMayCastContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for cascade may-cast")
        }

        if (!response.choice) {
            var newState = state
            val events = CascadeExecutor.bottomRandomize(
                services.zones,
                state = state,
                playerId = continuation.playerId,
                cards = continuation.exiledCards
            ) { newState = it }
            return checkForMore(newState, events)
        }

        // Yes — bottom the other exiled cards now, then attempt the free cast.
        val others = continuation.exiledCards.filter { it != continuation.cascadeCardId }
        var afterBottom = state
        val bottomEvents = CascadeExecutor.bottomRandomize(
            services.zones,
            state = state,
            playerId = continuation.playerId,
            cards = others
        ) { afterBottom = it }

        // A non-modal targeted spell can't carry targets through the synthesized CastSpell —
        // surface the ChooseTargetsDecision first, exactly as
        // CastFromCollectionWithoutPayingCostExecutor does. If a required slot has no legal
        // targets the cast can't initiate (CR 601.2c) and the cascade card is bottomed. Checked
        // *before* granting so the bottomed card carries no lingering free-cast grant.
        val targetPrep = CastFromCollectionWithoutPayingCostExecutor.prepareTargetSelection(
            state = afterBottom,
            cardId = continuation.cascadeCardId,
            casterId = continuation.playerId,
            cardRegistry = services.cardRegistry,
            targetFinder = targetFinder,
        )
        if (targetPrep is CastFromCollectionWithoutPayingCostExecutor.TargetPrep.NoLegalTargets) {
            var finalState = afterBottom
            val tailEvents = CascadeExecutor.bottomRandomize(
                services.zones,
                state = afterBottom,
                playerId = continuation.playerId,
                cards = listOf(continuation.cascadeCardId)
            ) { finalState = it }
            return checkForMore(finalState, bottomEvents + tailEvents)
        }

        // Grant free-cast permission so the synthesized cast pays nothing.
        val (permId, stateWithGrant) = CastFromCollectionWithoutPayingCostExecutor.grantFreeCast(
            state = afterBottom,
            cardId = continuation.cascadeCardId,
            controllerId = continuation.playerId,
            sourceId = continuation.sourceId,
        )

        if (targetPrep is CastFromCollectionWithoutPayingCostExecutor.TargetPrep.NeedsTargets) {
            val targetsContinuation = targetPrep.continuation.copy(
                grantedPermissionId = permId,
                onCastFailure = FreeCastFallback.BOTTOM_OF_LIBRARY,
            )
            return stateWithGrant.withPriority(continuation.playerId).suspendForDecision(
                question = targetPrep.question,
                answer = targetsContinuation,
                events = bottomEvents,
            )
        }

        // Hand priority to the cascade controller for the synthesized cast. The cast
        // happens *during* cascade resolution (CR 702.85a) rather than on a normal
        // priority window, so we override the priorityPlayerId for this single call.
        val stateForCast = stateWithGrant.copy(priorityPlayerId = continuation.playerId)
        val castAction = CastSpell(continuation.playerId, continuation.cascadeCardId)
        val castResult = castSpellHandler.execute(stateForCast, castAction)

        if (castResult.error != null) {
            // Cast couldn't initiate (no legal targets, etc.) — revoke the unused free-cast
            // grant; the cascade card wasn't cast, so it joins the leftovers on the bottom
            // of the library.
            val revoked = CastFromCollectionWithoutPayingCostExecutor.revokeFreeCast(
                stateWithGrant, continuation.cascadeCardId, permId
            )
            var finalState = revoked
            val tailEvents = CascadeExecutor.bottomRandomize(
                services.zones,
                state = revoked,
                playerId = continuation.playerId,
                cards = listOf(continuation.cascadeCardId)
            ) { finalState = it }
            return checkForMore(finalState, bottomEvents + tailEvents)
        }

        if (castResult.pendingDecision != null) {
            // The cast paused (for target / X / mode selection). The leftover
            // bottoming is already done; let the cast's own continuations finish
            // the cast on resume.
            return ExecutionResult.propagatePause(
                castResult.state,
                bottomEvents + castResult.events
            )
        }

        return checkForMore(castResult.state, bottomEvents + castResult.events)
    }

    /**
     * Resume after the controller answers "cast the discovered card for free, or put it into
     * your hand?" during a [com.wingedsheep.sdk.scripting.effects.DiscoverEffect] (CR 701.57a).
     *
     * In both branches the *other* exiled cards are bottom-randomized first. Then:
     *  - **Cast** (yes): the discovered card is granted a free cast (like [CascadeExecutor]) and
     *    synthesized through the normal cast machinery, so target / X / mode prompts surface and the
     *    cast's "whenever you cast a spell (from exile)" triggers are detected once, by the settle
     *    boundary. If the cast can't
     *    initiate — no legal target, etc. — the card falls back to the controller's hand, per
     *    "If you don't cast it, put that card into your hand."
     *  - **Hand** (no): the discovered card is moved straight to the controller's hand.
     *
     * Any [DiscoverMayCastContinuation.thenEffect] then resolves last, with the discovered card
     * published to [DiscoverMayCastContinuation.storeDiscoveredAs] so it can be read (Hit the
     * Mother Lode's "…create Treasure tokens equal to the difference"). In the cast branch it is
     * pre-pushed as an [EffectContinuation] so it runs after the cast even if the cast pauses.
     */
    fun resumeDiscoverMayCast(
        state: GameState,
        continuation: DiscoverMayCastContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for discover may-cast")
        }

        val discovered = continuation.discoveredCardId
        val others = continuation.exiledCards.filter { it != discovered }

        // Bottom-randomize every other exiled card first (CR 701.57a).
        var afterBottom = state
        val bottomEvents = CascadeExecutor.bottomRandomize(
            services.zones,
            state = state,
            playerId = continuation.playerId,
            cards = others
        ) { afterBottom = it }

        val discoveredCollections = continuation.storeDiscoveredAs
            ?.let { mapOf(it to listOf(discovered)) }
            ?: emptyMap()

        if (!response.choice) {
            // Put the discovered card into the controller's hand, then run the follow-up.
            val moveResult = ZoneMovementUtils.moveCardToZone(services.zones, afterBottom, discovered, Zone.HAND)
            var afterHand = afterBottom
            val leadingEvents = bottomEvents.toMutableList()
            if (moveResult.outcome is Outcome.Done) {
                afterHand = moveResult.state
                leadingEvents.addAll(moveResult.events)
            }
            return runDiscoverThenEffect(
                afterHand, continuation, discoveredCollections, leadingEvents, checkForMore
            )
        }

        // A non-modal targeted spell (Zombify) can't carry targets through the synthesized
        // CastSpell — surface the ChooseTargetsDecision first, exactly as
        // CastFromCollectionWithoutPayingCostExecutor does. If a required slot has no legal
        // targets the cast can't initiate (CR 601.2c) and the card goes to hand instead. Checked
        // *before* granting so the card reaches hand without a lingering free-cast grant.
        val targetPrep = CastFromCollectionWithoutPayingCostExecutor.prepareTargetSelection(
            state = afterBottom,
            cardId = discovered,
            casterId = continuation.playerId,
            cardRegistry = services.cardRegistry,
            targetFinder = targetFinder,
        )
        if (targetPrep is CastFromCollectionWithoutPayingCostExecutor.TargetPrep.NoLegalTargets) {
            val moveResult = ZoneMovementUtils.moveCardToZone(services.zones, afterBottom, discovered, Zone.HAND)
            var afterHand = afterBottom
            val handEvents = bottomEvents.toMutableList()
            if (moveResult.outcome is Outcome.Done) {
                afterHand = moveResult.state
                handEvents.addAll(moveResult.events)
            }
            return runDiscoverThenEffect(afterHand, continuation, discoveredCollections, handEvents, checkForMore)
        }

        // Cast branch: grant a free cast and synthesize it through the normal cast machinery —
        // mirroring CascadeExecutor's may-cast rather than the CastFromCollection effect, so the
        // cast's "whenever you cast a spell (from exile)" triggers are stacked exactly once
        // (Quintorius Kand).
        val (permId, granted) = CastFromCollectionWithoutPayingCostExecutor.grantFreeCast(
            state = afterBottom,
            cardId = discovered,
            controllerId = continuation.playerId,
            sourceId = continuation.sourceId,
        )

        // The follow-up [thenEffect] is pre-pushed as an EffectContinuation so it resolves after
        // the cast even if the cast pauses for targets / X.
        var stateForCast = granted
        if (continuation.thenEffect != null) {
            val thenCtx = EffectContext(
                sourceId = continuation.sourceId,
                objectReferences = continuation.objectReferences,
                controllerId = continuation.playerId,
                pipeline = PipelineState.EMPTY.copy(storedCollections = discoveredCollections)
            )
            stateForCast = stateForCast.pushContinuation(
                EffectContinuation(
                    remainingEffects = listOf(continuation.thenEffect),
                    effectContext = thenCtx
                )
            )
        }

        if (targetPrep is CastFromCollectionWithoutPayingCostExecutor.TargetPrep.NeedsTargets) {
            val targetsContinuation = targetPrep.continuation.copy(
                grantedPermissionId = permId,
                onCastFailure = FreeCastFallback.HAND,
            )
            return stateForCast.withPriority(continuation.playerId).suspendForDecision(
                question = targetPrep.question,
                answer = targetsContinuation,
                events = bottomEvents,
            )
        }

        val stateReady = stateForCast.copy(priorityPlayerId = continuation.playerId)
        val castResult = castSpellHandler.execute(stateReady, CastSpell(continuation.playerId, discovered))

        if (castResult.error != null) {
            // The cast couldn't initiate — pop the pre-pushed follow-up, revoke the unused
            // free-cast grant, put the discovered card into hand ("If you don't cast it, put
            // that card into your hand"), then run the follow-up (Hit the Mother Lode still
            // makes its Treasures — a card was discovered).
            val withoutThen = CastFromCollectionWithoutPayingCostExecutor.revokeFreeCast(
                if (continuation.thenEffect != null) stateForCast.popContinuation().second else stateForCast,
                discovered,
                permId,
            )
            val moveResult = ZoneMovementUtils.moveCardToZone(services.zones, withoutThen, discovered, Zone.HAND)
            var afterHand = withoutThen
            val handEvents = bottomEvents.toMutableList()
            if (moveResult.outcome is Outcome.Done) {
                afterHand = moveResult.state
                handEvents.addAll(moveResult.events)
            }
            return runDiscoverThenEffect(afterHand, continuation, discoveredCollections, handEvents, checkForMore)
        }

        if (castResult.pendingDecision != null) {
            // The cast paused (targets / X); the pre-pushed follow-up runs when it resumes.
            return ExecutionResult.propagatePause(castResult.state, bottomEvents + castResult.events)
        }

        // Cast succeeded synchronously; checkForMore drains the pre-pushed follow-up continuation
        // (the card's thenEffect plus the DiscoveredEvent emit tail).
        return checkForMore(castResult.state, bottomEvents + castResult.events)
    }

    /** Run a discover [DiscoverMayCastContinuation.thenEffect] (if any) with the discovered card published. */
    private fun runDiscoverThenEffect(
        state: GameState,
        continuation: DiscoverMayCastContinuation,
        discoveredCollections: Map<String, List<EntityId>>,
        leadingEvents: List<com.wingedsheep.engine.core.GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val thenEffect = continuation.thenEffect
            ?: return checkForMore(state, leadingEvents)
        val ctx = EffectContext(
            sourceId = continuation.sourceId,
            objectReferences = continuation.objectReferences,
            controllerId = continuation.playerId,
            pipeline = PipelineState.EMPTY.copy(storedCollections = discoveredCollections)
        )
        val result = effectRunner.executeRemainingEffects(state, listOf(thenEffect), ctx)
        if (result.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(result.state, leadingEvents + result.events)
        }
        return checkForMore(result.state, leadingEvents + result.events)
    }


    /**
     * Resume after the controller picks targets for a free synthesized cast triggered by
     * [com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect].
     *
     * Flattens the per-requirement target picks into a `List<ChosenTarget>` (via
     * [entityIdToChosenTarget]), invokes the normal cast pipeline, and bubbles any further
     * pause (X selection, modal-target prompts on a card that turned out to be modal, etc.)
     * through unchanged.
     */
    fun resumeCastFromCollectionTargets(
        state: GameState,
        continuation: CastFromCollectionTargetsContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(
                state,
                "Expected targets response for free-cast target selection"
            )
        }

        val chosenTargets = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityIdToChosenTarget(state, it) } }

        val stateForCast = state.copy(priorityPlayerId = continuation.casterId)
        val castResult = castSpellHandler.execute(
            stateForCast,
            CastSpell(continuation.casterId, continuation.cardId, chosenTargets, faceIndex = continuation.faceIndex),
        )

        if (castResult.error != null) {
            // Cast still couldn't initiate (e.g., targets became illegal between selection
            // and resolution). Revoke the unused free-cast grant and send the card to the
            // owning flow's fallback zone (discover → hand, cascade → bottom of library) so
            // it isn't stranded in exile; checkForMore keeps the rest of the trigger's
            // resolution (e.g. a discover follow-up frame) alive.
            var cleaned = CastFromCollectionWithoutPayingCostExecutor.revokeFreeCast(
                state, continuation.cardId, continuation.grantedPermissionId
            )
            val fallbackEvents = mutableListOf<GameEvent>()
            when (continuation.onCastFailure) {
                FreeCastFallback.LEAVE -> {}
                FreeCastFallback.HAND -> {
                    val moveResult = ZoneMovementUtils.moveCardToZone(services.zones, cleaned, continuation.cardId, Zone.HAND)
                    if (moveResult.outcome is Outcome.Done) {
                        cleaned = moveResult.state
                        fallbackEvents.addAll(moveResult.events)
                    }
                }
                FreeCastFallback.BOTTOM_OF_LIBRARY -> {
                    fallbackEvents.addAll(
                        CascadeExecutor.bottomRandomize(
                            services.zones,
                            state = cleaned,
                            playerId = continuation.casterId,
                            cards = listOf(continuation.cardId)
                        ) { cleaned = it }
                    )
                }
            }
            return checkForMore(cleaned, fallbackEvents)
        }

        // The cast initiated. Publish the cast card so an enclosing Effects.IfYouDo frame beneath
        // (Kaervek's "If you do, you lose 2 life") sees a non-empty collection.
        val castCollections = continuation.storeCastTo?.let { mapOf(it to listOf(continuation.cardId)) }
            ?: emptyMap()

        if (castResult.pendingDecision != null) {
            val exposed = exposeCollectionsToNextFrame(castResult.state, castCollections)
            return ExecutionResult.propagatePause(
                exposed,
                castResult.events,
            )
        }

        val exposed = exposeCollectionsToNextFrame(castResult.state, castCollections)
        return checkForMore(exposed, castResult.events)
    }

    /**
     * Resume a [CastAnyNumberFromCollectionContinuation] — one iteration of the
     * "cast any number of them for free" loop.
     *
     * The controller picked 0..1 cards from the still-castable set:
     *  - **0** → done; uncast cards stay in exile (no later-in-turn permission was granted).
     *  - **1** → cast it for free, then loop over the rest. Both steps run through
     *    [effectRunner]: it casts the single chosen card via
     *    [CastFromCollectionWithoutPayingCostEffect] (which handles target / X / mode pauses
     *    exactly as Cascade and Shiko do — and, going through `CastSpellHandler.execute`
     *    directly, ignores card-type timing) and then re-runs
     *    [CastAnyNumberFromCollectionWithoutPayingCostEffect] over the remaining cards. The
     *    runner's per-effect `EffectContinuation` makes a paused cast auto-resume into the
     *    next loop iteration.
     *
     * The chosen card is keyed under a private collection name and the loop collection is
     * trimmed to the remainder, so each iteration's bookkeeping is self-contained.
     *
     * A capped loop ("cast up to N of them") carries its remaining budget on the continuation;
     * the re-entered loop effect gets `maxCasts - 1` when the pick will actually be cast, and at
     * 0 the executor ends the loop without offering another decision. A pick that can't be cast
     * because a required target has no legal choice (CR 601.2c) leaves the budget alone — the
     * card is still dropped from the pool, so the loop can't re-offer it forever. The residual
     * case is a cast that initiates and then errors inside `CastSpellHandler`; that still spends
     * a cast, because the outcome isn't knowable before the loop's tail effect is built.
     */
    fun resumeCastAnyNumberFromCollection(
        state: GameState,
        continuation: CastAnyNumberFromCollectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for free-cast loop")
        }

        val ctx = continuation.effectContext
        val collection = ctx.pipeline.storedCollections[continuation.from].orEmpty()
        val chosenId = response.selectedCards.firstOrNull()

        // Declined, or a stale / no-longer-offered pick: end the loop. Uncast cards remain
        // wherever they are.
        if (chosenId == null || chosenId !in collection) {
            return checkForMore(state, emptyList())
        }

        val singleKey = "${continuation.from}\$next"
        val remaining = collection - chosenId
        val loopContext = ctx.copy(
            pipeline = ctx.pipeline.copy(
                storedCollections = ctx.pipeline.storedCollections +
                    (singleKey to listOf(chosenId)) +
                    (continuation.from to remaining)
            )
        )

        // "Up to N *spells*" is a cap on casts, not on picks: a chosen card whose required target
        // has no legal choice can't be cast at all (CR 601.2c) and
        // CastFromCollectionWithoutPayingCostExecutor no-ops on it, leaving it in exile. Ask the
        // executor's own precondition the same question it will ask, so that pick doesn't burn a
        // cast. Deterministic over the same state, so the two answers can't disagree.
        val castWillInitiate = (continuation.maxCasts == null && continuation.maxTotalManaValue == null) ||
            CastFromCollectionWithoutPayingCostExecutor.prepareTargetSelection(
                state = state,
                cardId = chosenId,
                casterId = ctx.controllerId,
                cardRegistry = services.cardRegistry,
                targetFinder = targetFinder,
            ) !is CastFromCollectionWithoutPayingCostExecutor.TargetPrep.NoLegalTargets

        val effects = listOf(
            CastFromCollectionWithoutPayingCostEffect(from = singleKey, payManaCost = continuation.payManaCost),
            CastAnyNumberFromCollectionWithoutPayingCostEffect(
                from = continuation.from,
                payManaCost = continuation.payManaCost,
                // One cast of the "up to N" budget has just been spent — unless nothing will be
                // cast, in which case the loop re-offers the rest with the budget intact (the
                // uncastable card is out of the pool either way, so this can't spin). `null`
                // stays uncapped; a budget that hits 0 makes the next iteration a no-op.
                maxCasts = continuation.maxCasts?.let { if (castWillInitiate) it - 1 else it },
                // "Total mana value N or less": the cast spends its mana value from the budget.
                maxTotalManaValue = continuation.maxTotalManaValue?.let { budget ->
                    if (castWillInitiate) {
                        budget - CastAnyNumberFromCollectionWithoutPayingCostExecutor.manaValueOf(state, chosenId)
                    } else budget
                },
            ),
        )
        val result = effectRunner.executeRemainingEffects(state, effects, loopContext)
        if (result.outcome is Outcome.Paused) return result.toExecutionResult()
        return checkForMore(result.state, result.events.toList())
    }
}
