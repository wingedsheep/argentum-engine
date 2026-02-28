package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.costs.PayCost

class SacrificeAndPayContinuationResumer(
    private val ctx: ContinuationContext
) {

    fun resumeSacrifice(
        state: GameState,
        continuation: SacrificeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // Move selected permanents from battlefield to owner's graveyard
        var newState = state
        val events = mutableListOf<GameEvent>()

        if (selectedPermanents.isNotEmpty()) {
            val permanentNames = selectedPermanents.map { id ->
                newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(PermanentsSacrificedEvent(playerId, selectedPermanents, permanentNames))
        }

        for (permanentId in selectedPermanents) {
            val container = newState.getEntity(permanentId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Find the actual zone (may differ from controller's zone for stolen creatures)
            val currentZone = newState.zones.entries.find { (_, cards) -> permanentId in cards }?.key
                ?: continue

            // Sacrificed cards go to their owner's graveyard
            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: playerId
            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

            newState = newState.removeFromZone(currentZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)

            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = cardComponent.name,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = ownerId
                )
            )
        }

        return checkForMore(newState, events)
    }

    fun resumePayOrSuffer(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val sourceName = continuation.sourceName

        return when (continuation.costType) {
            PayOrSufferCostType.DISCARD -> {
                if (continuation.random) {
                    resumePayOrSufferRandomDiscard(state, continuation, response, checkForMore)
                } else {
                    resumePayOrSufferDiscard(state, continuation, response, checkForMore)
                }
            }
            PayOrSufferCostType.SACRIFICE -> resumePayOrSufferSacrifice(state, continuation, response, checkForMore)
            PayOrSufferCostType.PAY_LIFE -> resumePayOrSufferPayLife(state, continuation, response, checkForMore)
        }
    }

    /**
     * Handle discard cost selection for pay or suffer.
     */
    private fun resumePayOrSufferDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer discard")
        }

        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val selectedCards = response.selectedCards

        // If player didn't select enough cards, execute the suffer effect
        if (selectedCards.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost - discard the selected cards
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (cardId in selectedCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.HAND,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        val discardNames = selectedCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        events.add(0, CardsDiscardedEvent(playerId, selectedCards, discardNames))
        return checkForMore(newState, events)
    }

    /**
     * Handle random discard yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferRandomDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer random discard")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player chose to pay - execute random discard
        val result = com.wingedsheep.engine.handlers.effects.removal.PayOrSufferExecutor.executeRandomDiscard(
            state,
            continuation.playerId,
            continuation.filter,
            continuation.requiredCount
        )
        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Handle sacrifice cost selection for pay or suffer.
     */
    private fun resumePayOrSufferSacrifice(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // If player didn't select enough permanents, execute the suffer effect
        if (selectedPermanents.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost - sacrifice the selected permanents
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (permanentId in selectedPermanents) {
            val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)
            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = permanentName,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        val permanentNames = selectedPermanents.map { id ->
            state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
        }
        events.add(0, PermanentsSacrificedEvent(playerId, selectedPermanents, permanentNames))
        return checkForMore(newState, events)
    }

    /**
     * Handle pay life yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferPayLife(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer pay life")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player chose to pay life
        val playerId = continuation.playerId
        val lifeToPay = continuation.requiredCount
        val playerContainer = state.getEntity(playerId)
        val currentLife = playerContainer?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
        val newLife = currentLife - lifeToPay

        val newState = state.updateEntity(playerId) {
            it.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
        }

        val events = listOf(
            LifeChangedEvent(
                playerId = playerId,
                oldLife = currentLife,
                newLife = newLife,
                reason = LifeChangeReason.PAYMENT
            )
        )

        return checkForMore(newState, events)
    }

    private fun executePayOrSufferConsequence(
        state: GameState,
        continuation: PayOrSufferContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val sourceId = continuation.sourceId
        val playerId = continuation.playerId
        val sufferEffect = continuation.sufferEffect

        // Create context for executing the suffer effect, preserving targets from the original trigger
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = playerId,
            opponentId = null,
            targets = continuation.targets,
            namedTargets = continuation.namedTargets
        )

        // Execute the suffer effect using the registry
        val result = ctx.effectExecutorRegistry.execute(state, sufferEffect, context)

        return if (result.isPaused) {
            result
        } else {
            checkForMore(result.state, result.events.toList())
        }
    }

    /**
     * Resume after a player decides whether to pay for "any player may [cost]" effects.
     */
    fun resumeAnyPlayerMayPay(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for any player may pay")
        }

        val selectedPermanents = response.selectedCards
        val playerId = continuation.currentPlayerId

        // If player selected enough permanents, they paid the cost
        if (selectedPermanents.size >= continuation.requiredCount) {
            // Sacrifice the selected permanents
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (permanentId in selectedPermanents) {
                val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
                newState = newState.removeFromZone(battlefieldZone, permanentId)
                newState = newState.addToZone(graveyardZone, permanentId)
                events.add(
                    ZoneChangeEvent(
                        entityId = permanentId,
                        entityName = permanentName,
                        fromZone = Zone.BATTLEFIELD,
                        toZone = Zone.GRAVEYARD,
                        ownerId = playerId
                    )
                )
            }
            events.add(0, PermanentsSacrificedEvent(playerId, selectedPermanents))

            // Now execute the consequence (e.g., sacrifice the source)
            val context = EffectContext(
                sourceId = continuation.sourceId,
                controllerId = continuation.controllerId,
                opponentId = null
            )
            val consequenceResult = ctx.effectExecutorRegistry.execute(newState, continuation.consequence, context)
            val allEvents = events + consequenceResult.events

            return if (consequenceResult.isPaused) {
                consequenceResult
            } else {
                checkForMore(consequenceResult.state, allEvents)
            }
        }

        // Player declined - find next player who can pay
        return askNextPlayerForAnyPlayerMayPay(state, continuation, checkForMore)
    }

    /**
     * Find and ask the next eligible player for "any player may [cost]" effects.
     * If no player can pay, nothing happens and execution continues.
     */
    private fun askNextPlayerForAnyPlayerMayPay(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val predicateEvaluator = PredicateEvaluator()
        val cost = continuation.cost
        val projected = com.wingedsheep.engine.mechanics.layers.StateProjector().project(state)

        // Find next player who can pay among remaining players
        for ((index, nextPlayerId) in continuation.remainingPlayers.withIndex()) {
            if (cost is PayCost.Sacrifice) {
                val battlefieldZone = ZoneKey(nextPlayerId, Zone.BATTLEFIELD)
                val battlefield = state.getZone(battlefieldZone)
                val context = PredicateContext(controllerId = nextPlayerId)

                val validPermanents = battlefield.filter { permanentId ->
                    predicateEvaluator.matchesWithProjection(state, projected, permanentId, cost.filter, context)
                }

                if (validPermanents.size >= cost.count) {
                    // This player can pay - ask them
                    val prompt = "You may sacrifice ${cost.count} ${cost.filter.description}s to cause ${continuation.sourceName} to be sacrificed, or skip"
                    val decisionHandler = DecisionHandler()

                    val decisionResult = decisionHandler.createCardSelectionDecision(
                        state = state,
                        playerId = nextPlayerId,
                        sourceId = continuation.sourceId,
                        sourceName = continuation.sourceName,
                        prompt = prompt,
                        options = validPermanents,
                        minSelections = 0,
                        maxSelections = cost.count,
                        ordered = false,
                        phase = DecisionPhase.RESOLUTION,
                        useTargetingUI = true
                    )

                    val newContinuation = continuation.copy(
                        decisionId = decisionResult.pendingDecision!!.id,
                        currentPlayerId = nextPlayerId,
                        remainingPlayers = continuation.remainingPlayers.drop(index + 1)
                    )

                    val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)

                    return ExecutionResult.paused(
                        stateWithContinuation,
                        decisionResult.pendingDecision,
                        decisionResult.events
                    )
                }
            }
        }

        // No player can pay - nothing happens, Pangolin stays
        return checkForMore(state, emptyList())
    }

    fun resumeUntapChoice(
        state: GameState,
        continuation: UntapChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for untap choice")
        }

        val keepTapped = response.selectedCards.toSet()
        val toUntap = continuation.allPermanentsToUntap.filter { it !in keepTapped }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Untap the permanents that the player did NOT choose to keep tapped
        for (entityId in toUntap) {
            val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
            events.add(UntappedEvent(entityId, cardName))
        }

        // Remove WhileSourceTapped floating effects whose source is no longer tapped
        newState = newState.copy(
            floatingEffects = newState.floatingEffects.filter { floatingEffect ->
                if (floatingEffect.duration is Duration.WhileSourceTapped) {
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId) &&
                        newState.getEntity(sourceId)?.has<TappedComponent>() == true
                } else {
                    true
                }
            }
        )

        // Remove summoning sickness from all creatures the active player controls
        val activePlayer = continuation.playerId
        val projected = com.wingedsheep.engine.mechanics.layers.StateProjector().project(newState)
        val creaturesToRefresh = newState.entities.filter { (entityId, container) ->
            projected.getController(entityId) == activePlayer &&
                container.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
        }.keys

        for (entityId in creaturesToRefresh) {
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
