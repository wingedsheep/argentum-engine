package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

class LibraryAndZoneContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ReturnFromGraveyardContinuation::class, ::resumeReturnFromGraveyard),
        resumer(MoveCollectionOrderContinuation::class, ::resumeMoveCollectionOrder),
        resumer(PutOnBottomOfLibraryContinuation::class, ::resumePutOnBottomOfLibrary),
        resumer(PutFromHandContinuation::class, ::resumePutFromHand),
        resumer(SelectFromCollectionContinuation::class, ::resumeSelectFromCollection),
        resumer(SelectTargetPipelineContinuation::class, ::resumeSelectTargetPipeline),
        resumer(MoveCollectionAuraTargetContinuation::class, ::resumeMoveCollectionAuraTarget),
        resumer(PutOnTopOrBottomContinuation::class, ::resumePutOnTopOrBottom)
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
        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
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

        // Remove all cards from their current zones
        for (cardId in orderedCards) {
            val ownerId = newState.getEntity(cardId)?.get<OwnerComponent>()?.playerId ?: destPlayerId
            for (zone in Zone.entries) {
                val zoneKey = ZoneKey(ownerId, zone)
                if (cardId in newState.getZone(zoneKey)) {
                    newState = newState.removeFromZone(zoneKey, cardId)
                    break
                }
            }
        }

        // Place cards in library in the chosen order
        val currentLibrary = newState.getZone(libraryZone)
        newState = if (continuation.placement == ZonePlacement.Bottom) {
            // Bottom: append ordered cards at the end
            newState.copy(
                zones = newState.zones + (libraryZone to currentLibrary + orderedCards)
            )
        } else {
            // Top (default): prepend ordered cards at the beginning
            newState.copy(
                zones = newState.zones + (libraryZone to orderedCards + currentLibrary)
            )
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

        // Get current library
        val currentLibrary = state.getZone(libraryZone).toMutableList()

        // Remove the reordered cards from the library (they should already be removed by the executor,
        // but filter just in case)
        val cardsSet = orderedCards.toSet()
        val remainingLibrary = currentLibrary.filter { it !in cardsSet }

        // Place the cards on the BOTTOM in the player's chosen order
        val newLibrary = remainingLibrary + orderedCards

        // Update the library zone
        val newState = state.copy(
            zones = state.zones + (libraryZone to newLibrary)
        )

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
        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
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
        val auraId = continuation.auraId
        val destPlayerId = continuation.destPlayerId

        // Use MoveCollectionExecutor's helper to move aura to battlefield with attachment
        val executor = com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor(
            cardRegistry = services.cardRegistry,
            targetFinder = services.targetFinder
        )
        val (newState, moveEvents) = executor.moveAuraToBattlefield(state, auraId, targetId, destPlayerId)

        // Continue with remaining auras
        val remainingAuras = continuation.remainingAuras
        if (remainingAuras.isNotEmpty()) {
            val nextAuraId = remainingAuras.first()
            val nextRemaining = remainingAuras.drop(1)

            val nextCardComponent = newState.getEntity(nextAuraId)?.get<CardComponent>()
            val nextCardDef = nextCardComponent?.let { services.cardRegistry.getCard(it.cardDefinitionId) }
            val nextAuraTarget = nextCardDef?.script?.auraTarget

            if (nextAuraTarget == null) {
                // Skip this aura, continue to next
                return resumeMoveCollectionAuraTarget(
                    newState,
                    continuation.copy(
                        auraId = nextAuraId,
                        remainingAuras = nextRemaining,
                        decisionId = "skip"
                    ),
                    response,
                    checkForMore
                )
            }

            val legalTargets = services.targetFinder.findLegalTargets(
                state = newState,
                requirement = nextAuraTarget,
                controllerId = continuation.controllerId,
                sourceId = nextAuraId,
                ignoreTargetingRestrictions = true
            )

            if (legalTargets.isEmpty()) {
                // No targets - skip and continue
                if (nextRemaining.isNotEmpty()) {
                    return resumeMoveCollectionAuraTarget(
                        newState,
                        continuation.copy(
                            auraId = nextRemaining.first(),
                            remainingAuras = nextRemaining.drop(1),
                            decisionId = "skip"
                        ),
                        response,
                        checkForMore
                    )
                }
                return checkForMore(newState, moveEvents)
            }

            // Pause for next aura target
            val decisionId = java.util.UUID.randomUUID().toString()
            val auraName = nextCardComponent.name
            val requirementInfo = TargetRequirementInfo(
                index = 0,
                description = nextAuraTarget.description,
                minTargets = 1,
                maxTargets = 1
            )
            val decision = ChooseTargetsDecision(
                id = decisionId,
                playerId = continuation.controllerId,
                prompt = "Choose what $auraName enchants",
                context = DecisionContext(
                    sourceId = nextAuraId,
                    sourceName = auraName,
                    phase = DecisionPhase.RESOLUTION
                ),
                targetRequirements = listOf(requirementInfo),
                legalTargets = mapOf(0 to legalTargets)
            )

            val nextContinuation = MoveCollectionAuraTargetContinuation(
                decisionId = decisionId,
                auraId = nextAuraId,
                controllerId = continuation.controllerId,
                destPlayerId = destPlayerId,
                remainingAuras = nextRemaining,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName
            )

            val stateWithDecision = newState.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(nextContinuation)

            return ExecutionResult(
                state = stateWithContinuation,
                events = moveEvents,
                pendingDecision = decision
            )
        }

        return checkForMore(newState, moveEvents)
    }

    fun resumeSelectFromCollection(
        state: GameState,
        continuation: SelectFromCollectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
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
            for (cardId in response.selectedCards) {
                val acceptsAllRestrictions = continuation.restrictions.all { restriction ->
                    when (restriction) {
                        is SelectionRestriction.OnePerCardType -> {
                            val cardTypes = state.getEntity(cardId)
                                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                                ?.typeLine?.cardTypes ?: emptySet()
                            cardTypes.isEmpty() || cardTypes.none { it in claimedTypes }
                        }
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

        // Inject updated collections into the next EffectContinuation on the stack (if present)
        val nextFrame = state.peekContinuation()
        val newState = if (nextFrame is EffectContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            stateAfterPop.pushContinuation(
                nextFrame.copy(effectContext = nextFrame.effectContext.copy(pipeline = nextFrame.effectContext.pipeline.copy(storedCollections = updatedCollections)))
            )
        } else {
            state
        }

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

        // Inject updated collections into the next EffectContinuation on the stack (if present)
        val nextFrame = state.peekContinuation()
        val newState = if (nextFrame is EffectContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            stateAfterPop.pushContinuation(
                nextFrame.copy(effectContext = nextFrame.effectContext.copy(pipeline = nextFrame.effectContext.pipeline.copy(storedCollections = updatedCollections)))
            )
        } else {
            state
        }

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after a card's owner chose top or bottom of their library.
     * Moves the card to the chosen position via ZoneTransitionService, or — if the
     * target is a spell on the stack — counters the spell and places it directly
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

        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
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
        val newLibrary = when (placement) {
            com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top ->
                listOf(spellId) + currentLibrary
            com.wingedsheep.engine.handlers.effects.LibraryPlacement.Bottom ->
                currentLibrary + spellId
            is com.wingedsheep.engine.handlers.effects.LibraryPlacement.NthFromTop -> {
                val insertIndex = placement.position.coerceAtMost(currentLibrary.size)
                currentLibrary.toMutableList().apply { add(insertIndex, spellId) }
            }
            else -> currentLibrary + spellId
        }
        newState = newState.copy(zones = newState.zones + (libZoneKey to newLibrary))

        // Both players watched the spell get placed at this position — mark it revealed to all
        // so each side's library viewer shows it face-up at the new slot.
        newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .markRevealed(newState, listOf(spellId), newState.turnOrder.toSet())

        val events = listOf(
            SpellCounteredEvent(spellId, spellName),
            ZoneChangeEvent(
                entityId = spellId,
                entityName = spellName,
                fromZone = Zone.STACK,
                toZone = Zone.LIBRARY,
                ownerId = ownerId
            )
        )
        return checkForMore(newState, events)
    }
}
