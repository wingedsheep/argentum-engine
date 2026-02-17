package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter

class MultiPlayerContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeEachOpponentMayPutFromHand(
        state: GameState,
        continuation: EachOpponentMayPutFromHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-opponent-may-put-from-hand")
        }

        var newState = state
        val events = mutableListOf<GameEvent>()
        val opponentId = continuation.currentOpponentId
        val handZone = ZoneKey(opponentId, Zone.HAND)
        val battlefieldZone = ZoneKey(opponentId, Zone.BATTLEFIELD)

        // Separate auras from non-auras: auras need target selection before entering
        val auraCards = mutableListOf<EntityId>()
        val nonAuraCards = mutableListOf<EntityId>()

        for (cardId in response.selectedCards) {
            if (cardId !in newState.getZone(handZone)) continue
            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            if (cardComponent?.isAura == true) {
                auraCards.add(cardId)
            } else {
                nonAuraCards.add(cardId)
            }
        }

        // Move non-aura cards from hand to battlefield immediately
        for (cardId in nonAuraCards) {
            if (cardId !in newState.getZone(handZone)) continue

            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(battlefieldZone, cardId)

            // Apply battlefield components
            val container = newState.getEntity(cardId)
            if (container != null) {
                var newContainer = container
                    .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(opponentId))

                // Creatures enter with summoning sickness
                val cardComponent = container.get<CardComponent>()
                if (cardComponent?.typeLine?.isCreature == true) {
                    newContainer = newContainer.with(
                        com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                    )
                }

                newState = newState.copy(
                    entities = newState.entities + (cardId to newContainer)
                )
            }

            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            val cardName = cardComponent?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.HAND,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = opponentId
                )
            )
        }

        // If there are auras to place, ask for target selection for the first one
        if (auraCards.isNotEmpty()) {
            return askAuraTargetForEntryFromHand(
                state = newState,
                events = events,
                auraId = auraCards.first(),
                opponentId = opponentId,
                remainingAuras = auraCards.drop(1),
                remainingOpponents = continuation.remainingOpponents,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                controllerId = continuation.controllerId,
                filter = continuation.filter,
                checkForMore = checkForMore
            )
        }

        // Ask the next opponent
        return proceedToNextOpponentOrFinish(
            state = newState,
            events = events,
            remainingOpponents = continuation.remainingOpponents,
            filter = continuation.filter,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            checkForMore = checkForMore
        )
    }

    /**
     * Ask the opponent to choose a target for an Aura entering the battlefield from hand.
     * Per Rule 303.4f, the controller chooses what it enchants.
     * If no legal target exists, the Aura stays in hand (Rule 303.4g).
     */
    private fun askAuraTargetForEntryFromHand(
        state: GameState,
        events: List<GameEvent>,
        auraId: EntityId,
        opponentId: EntityId,
        remainingAuras: List<EntityId>,
        remainingOpponents: List<EntityId>,
        sourceId: EntityId?,
        sourceName: String?,
        controllerId: EntityId,
        filter: GameObjectFilter,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val cardComponent = state.getEntity(auraId)?.get<CardComponent>()
        val cardDef = cardComponent?.let { ctx.stackResolver.cardRegistry?.getCard(it.cardDefinitionId) }
        val auraTarget = cardDef?.script?.auraTarget

        if (auraTarget == null) {
            // No aura target defined — skip this aura (leave in hand)
            return continueAuraProcessingOrProceed(
                state = state,
                events = events,
                remainingAuras = remainingAuras,
                opponentId = opponentId,
                remainingOpponents = remainingOpponents,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter,
                checkForMore = checkForMore
            )
        }

        val legalTargets = ctx.targetFinder.findLegalTargets(
            state = state,
            requirement = auraTarget,
            controllerId = opponentId,
            sourceId = auraId
        )

        if (legalTargets.isEmpty()) {
            // No legal targets — Aura stays in hand per Rule 303.4g
            return continueAuraProcessingOrProceed(
                state = state,
                events = events,
                remainingAuras = remainingAuras,
                opponentId = opponentId,
                remainingOpponents = remainingOpponents,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter,
                checkForMore = checkForMore
            )
        }

        // Create target selection decision for the aura
        val decisionId = java.util.UUID.randomUUID().toString()
        val auraName = cardComponent.name
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = auraTarget.description,
            minTargets = 1,
            maxTargets = 1
        )
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = opponentId,
            prompt = "Choose what $auraName enchants",
            context = DecisionContext(
                sourceId = auraId,
                sourceName = auraName,
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(requirementInfo),
            legalTargets = mapOf(0 to legalTargets)
        )

        val auraContinuation = ChooseAuraTargetForEntryFromHandContinuation(
            decisionId = decisionId,
            auraId = auraId,
            opponentId = opponentId,
            remainingAuras = remainingAuras,
            remainingOpponents = remainingOpponents,
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = controllerId,
            filter = filter
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(auraContinuation)

        return ExecutionResult(
            state = stateWithContinuation,
            events = events,
            pendingDecision = decision
        )
    }

    /**
     * Continue processing remaining auras, or proceed to next opponent if no more auras.
     */
    private fun continueAuraProcessingOrProceed(
        state: GameState,
        events: List<GameEvent>,
        remainingAuras: List<EntityId>,
        opponentId: EntityId,
        remainingOpponents: List<EntityId>,
        sourceId: EntityId?,
        sourceName: String?,
        controllerId: EntityId,
        filter: GameObjectFilter,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (remainingAuras.isNotEmpty()) {
            return askAuraTargetForEntryFromHand(
                state = state,
                events = events,
                auraId = remainingAuras.first(),
                opponentId = opponentId,
                remainingAuras = remainingAuras.drop(1),
                remainingOpponents = remainingOpponents,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter,
                checkForMore = checkForMore
            )
        }

        return proceedToNextOpponentOrFinish(
            state = state,
            events = events,
            remainingOpponents = remainingOpponents,
            filter = filter,
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = controllerId,
            checkForMore = checkForMore
        )
    }

    /**
     * Ask the next opponent to put cards from hand, or finish if no more opponents.
     */
    private fun proceedToNextOpponentOrFinish(
        state: GameState,
        events: List<GameEvent>,
        remainingOpponents: List<EntityId>,
        filter: GameObjectFilter,
        sourceId: EntityId?,
        sourceName: String?,
        controllerId: EntityId,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (remainingOpponents.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachOpponentMayPutFromHandExecutor.askNextOpponent(
                state = state,
                filter = filter,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                opponents = remainingOpponents,
                currentIndex = 0
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = events + nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        return checkForMore(state, events)
    }

    /**
     * Resume after an opponent chose a target for an Aura entering the battlefield from hand.
     * Moves the aura from hand to battlefield with AttachedToComponent, then continues
     * processing remaining auras or the next opponent.
     */
    fun resumeChooseAuraTargetForEntryFromHand(
        state: GameState,
        continuation: ChooseAuraTargetForEntryFromHandContinuation,
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
        val opponentId = continuation.opponentId
        val handZone = ZoneKey(opponentId, Zone.HAND)
        val battlefieldZone = ZoneKey(opponentId, Zone.BATTLEFIELD)

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Verify aura is still in hand
        if (auraId !in newState.getZone(handZone)) {
            // Aura is no longer in hand — skip
            return continueAuraProcessingOrProceed(
                state = newState,
                events = events,
                remainingAuras = continuation.remainingAuras,
                opponentId = opponentId,
                remainingOpponents = continuation.remainingOpponents,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                controllerId = continuation.controllerId,
                filter = continuation.filter,
                checkForMore = checkForMore
            )
        }

        // Move aura from hand to battlefield
        newState = newState.removeFromZone(handZone, auraId)
        newState = newState.addToZone(battlefieldZone, auraId)

        // Apply battlefield components + AttachedToComponent
        val container = newState.getEntity(auraId)
        if (container != null) {
            val newContainer = container
                .with(ControllerComponent(opponentId))
                .with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(targetId))

            newState = newState.copy(
                entities = newState.entities + (auraId to newContainer)
            )
        }

        val cardComponent = newState.getEntity(auraId)?.get<CardComponent>()
        val cardName = cardComponent?.name ?: "Unknown"
        events.add(
            ZoneChangeEvent(
                entityId = auraId,
                entityName = cardName,
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = opponentId
            )
        )

        // Continue with remaining auras or next opponent
        return continueAuraProcessingOrProceed(
            state = newState,
            events = events,
            remainingAuras = continuation.remainingAuras,
            opponentId = opponentId,
            remainingOpponents = continuation.remainingOpponents,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            filter = continuation.filter,
            checkForMore = checkForMore
        )
    }

    /**
     * Resume after a player selected creature cards from their hand to reveal.
     *
     * Records the reveal count, then asks the next player (if any).
     * After all players have selected, creates tokens for each player.
     */
    fun resumeEachPlayerMayRevealCreatures(
        state: GameState,
        continuation: EachPlayerMayRevealCreaturesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-player-may-reveal-creatures")
        }

        val playerId = continuation.currentPlayerId
        val revealCount = response.selectedCards.size

        // Update reveal counts
        val updatedRevealCounts = if (revealCount > 0) {
            continuation.revealCounts + (playerId to revealCount)
        } else {
            continuation.revealCounts
        }

        // Ask the next player
        val remainingPlayers = continuation.remainingPlayers
        if (remainingPlayers.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerMayRevealCreaturesExecutor.askNextPlayer(
                state = state,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                players = remainingPlayers,
                currentIndex = 0,
                revealCounts = updatedRevealCounts,
                tokenPower = continuation.tokenPower,
                tokenToughness = continuation.tokenToughness,
                tokenColors = continuation.tokenColors,
                tokenCreatureTypes = continuation.tokenCreatureTypes,
                tokenImageUri = continuation.tokenImageUri
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        // All players have made their selection - create tokens
        val tokenResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerMayRevealCreaturesExecutor.createTokensForAllPlayers(
            state = state,
            revealCounts = updatedRevealCounts,
            tokenPower = continuation.tokenPower,
            tokenToughness = continuation.tokenToughness,
            tokenColors = continuation.tokenColors,
            tokenCreatureTypes = continuation.tokenCreatureTypes,
            tokenImageUri = continuation.tokenImageUri
        )

        return checkForMore(tokenResult.newState, tokenResult.events)
    }

    /**
     * Resume after a player selects cards from library in "each player searches library" effect.
     *
     * Moves selected cards to hand, reveals them, shuffles library, then asks the next player.
     */
    fun resumeEachPlayerSearchesLibrary(
        state: GameState,
        continuation: EachPlayerSearchesLibraryContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-player-searches-library")
        }

        val playerId = continuation.currentPlayerId
        val selectedCards = response.selectedCards
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)
        val events = mutableListOf<GameEvent>()

        var newState = state

        // Move selected cards from library to hand
        for (cardId in selectedCards) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)
        }

        // Reveal cards if any were selected
        if (selectedCards.isNotEmpty()) {
            val cardNames = selectedCards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = selectedCards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = selectedCards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = continuation.sourceName
                )
            )
            for (i in selectedCards.indices) {
                events.add(
                    ZoneChangeEvent(
                        entityId = selectedCards[i],
                        entityName = cardNames[i],
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.HAND,
                        ownerId = playerId
                    )
                )
            }
        }

        // Shuffle the library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))
        events.add(LibraryShuffledEvent(playerId))

        // Ask next player
        val remainingPlayers = continuation.remainingPlayers
        if (remainingPlayers.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerSearchesLibraryExecutor.askNextPlayer(
                state = newState,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                players = remainingPlayers,
                currentIndex = 0,
                filter = continuation.filter,
                maxCount = continuation.maxCount
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = events + nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        // All players done
        return checkForMore(newState, events)
    }

    fun resumeRevealAndOpponentChooses(
        state: GameState,
        continuation: RevealAndOpponentChoosesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for reveal and opponent chooses")
        }

        val controllerId = continuation.controllerId
        val selectedCard = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No card selected by opponent")

        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
        val events = mutableListOf<GameEvent>()

        var newState = state

        for (cardId in continuation.allCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            // Remove from library
            newState = newState.removeFromZone(libraryZone, cardId)

            if (cardId == selectedCard) {
                // Selected card goes to battlefield
                newState = newState.addToZone(battlefieldZone, cardId)

                // Apply battlefield components (controller, summoning sickness)
                val container = newState.getEntity(cardId)
                if (container != null) {
                    var newContainer = container
                        .with(com.wingedsheep.engine.state.components.identity.ControllerComponent(controllerId))

                    val cardComponent = container.get<CardComponent>()
                    if (cardComponent?.typeLine?.isCreature == true) {
                        newContainer = newContainer.with(
                            com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
                        )
                    }

                    newState = newState.copy(
                        entities = newState.entities + (cardId to newContainer)
                    )
                }

                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.BATTLEFIELD,
                        ownerId = controllerId
                    )
                )
            } else {
                // Non-selected cards go to graveyard
                newState = newState.addToZone(graveyardZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.GRAVEYARD,
                        ownerId = controllerId
                    )
                )
            }
        }

        return checkForMore(newState, events)
    }
}
