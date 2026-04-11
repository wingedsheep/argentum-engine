package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.zones.ForceExileMultiZoneExecutor
import com.wingedsheep.engine.handlers.effects.zones.ForceSacrificeExecutor
import com.wingedsheep.engine.handlers.effects.zones.ForceReturnOwnPermanentExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect

class SacrificeAndPayContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(SacrificeContinuation::class, ::resumeSacrifice),
        resumer(ReturnToHandContinuation::class, ::resumeReturnToHand),
        resumer(ExileMultiZoneContinuation::class, ::resumeExileMultiZone),
        resumer(PayOrSufferContinuation::class, ::resumePayOrSuffer),
        resumer(PayOrSufferChoiceContinuation::class, ::resumePayOrSufferChoice),
        resumer(AnyPlayerMayPayContinuation::class, ::resumeAnyPlayerMayPay),
        resumer(UntapChoiceContinuation::class, ::resumeUntapChoice)
    )

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
            newState = ZoneTransitionService.trackFoodSacrifice(newState, selectedPermanents, playerId)
        }

        for (permanentId in selectedPermanents) {
            val transitionResult = ZoneTransitionService.moveToZone(
                newState, permanentId, Zone.GRAVEYARD
            )
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

        // If there are remaining players (from "each opponent" effects), process them
        if (continuation.remainingPlayers.isNotEmpty() && continuation.filter != null) {
            val executor = ForceSacrificeExecutor()
            val result = executor.processPlayers(
                newState, continuation.remainingPlayers, continuation.filter,
                continuation.count, continuation.sourceId
            )
            val allEvents = events + result.events
            return if (result.isPaused) {
                // Another player needs a decision — return paused with combined events
                ExecutionResult.paused(result.state, result.pendingDecision!!, allEvents)
            } else {
                checkForMore(result.state, allEvents)
            }
        }

        return checkForMore(newState, events)
    }

    fun resumeReturnToHand(
        state: GameState,
        continuation: ReturnToHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for return to hand")
        }

        val selectedPermanent = response.selectedCards.firstOrNull()
            ?: return checkForMore(state, emptyList())

        val executor = ForceReturnOwnPermanentExecutor()
        val result = executor.returnPermanentToHand(state, selectedPermanent)

        return checkForMore(result.state, result.events.toList())
    }

    fun resumeExileMultiZone(
        state: GameState,
        continuation: ExileMultiZoneContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for multi-zone exile")
        }

        val result = ForceExileMultiZoneExecutor.exileEntities(
            state, continuation.playerId, response.selectedCards
        )

        return checkForMore(result.state, result.events.toList())
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
            PayOrSufferCostType.MANA -> resumePayOrSufferMana(state, continuation, response, checkForMore)
            PayOrSufferCostType.EXILE -> resumePayOrSufferExile(state, continuation, response, checkForMore)
            PayOrSufferCostType.CHOICE -> ExecutionResult.error(state, "Choice cost type should be handled by PayOrSufferChoiceContinuation, not PayOrSufferContinuation")
        }
    }

    /**
     * Resume after player picks which cost to pay from a multi-option PayOrSufferEffect.
     */
    fun resumePayOrSufferChoice(
        state: GameState,
        continuation: PayOrSufferChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for pay or suffer choice")
        }

        val chosenIndex = response.optionIndex

        // Last option is always the suffer effect
        if (chosenIndex >= continuation.options.size) {
            // Player chose the suffer option
            val context = EffectContext(
                sourceId = continuation.sourceId,
                controllerId = continuation.playerId,
                opponentId = null,
                targets = continuation.targets,
                pipeline = PipelineState(namedTargets = continuation.namedTargets)
            )
            val result = services.effectExecutorRegistry.execute(state, continuation.sufferEffect, context)
            return if (result.isPaused) result else checkForMore(result.state, result.events.toList())
        }

        // Player chose a cost option — create a single-cost PayOrSufferEffect and execute it
        val chosenCost = continuation.options[chosenIndex]
        val singleCostEffect = PayOrSufferEffect(
            cost = chosenCost,
            suffer = continuation.sufferEffect
        )
        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.playerId,
            opponentId = null,
            targets = continuation.targets,
            pipeline = PipelineState(namedTargets = continuation.namedTargets)
        )
        val result = services.effectExecutorRegistry.execute(state, singleCostEffect, context)
        return if (result.isPaused) result else checkForMore(result.state, result.events.toList())
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
        val result = com.wingedsheep.engine.handlers.effects.player.PayOrSufferExecutor.executeRandomDiscard(
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
        var newState = state
        val events = mutableListOf<GameEvent>()

        val permanentNames = selectedPermanents.map { id ->
            newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
        }
        events.add(PermanentsSacrificedEvent(playerId, selectedPermanents, permanentNames))

        for (permanentId in selectedPermanents) {
            val transitionResult = ZoneTransitionService.moveToZone(
                newState, permanentId, Zone.GRAVEYARD
            )
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

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

        var newState = state.updateEntity(playerId) {
            it.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
        }
        newState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(newState, playerId)

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

    /**
     * Handle exile cost selection for pay or suffer.
     */
    private fun resumePayOrSufferExile(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer exile")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // If player didn't select enough cards, execute the suffer effect
        if (selectedCards.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost - exile the selected cards
        val sourceZone = continuation.zone ?: Zone.HAND
        val fromZone = ZoneKey(playerId, sourceZone)
        val exileZone = ZoneKey(playerId, Zone.EXILE)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (cardId in selectedCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(fromZone, cardId)
            newState = newState.addToZone(exileZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = sourceZone,
                    toZone = Zone.EXILE,
                    ownerId = playerId
                )
            )
        }

        return checkForMore(newState, events)
    }

    /**
     * Handle mana cost yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferMana(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer mana")
        }

        if (!response.choice) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player chose to pay — auto-tap sources and deduct mana
        val manaCost = continuation.manaCost
            ?: return ExecutionResult.error(state, "No mana cost stored in continuation")
        val playerId = continuation.playerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white,
            manaPoolComponent.blue,
            manaPoolComponent.black,
            manaPoolComponent.red,
            manaPoolComponent.green,
            manaPoolComponent.colorless
        )

        // Try to pay from floating mana first, then tap sources for the rest
        val partialResult = manaPool.payPartial(manaCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            val manaSolver = ManaSolver(services.cardRegistry)
            val solution = manaSolver.solve(currentState, playerId, remainingCost)
                ?: return executePayOrSufferConsequence(state, continuation, checkForMore)

            for (source in solution.sources) {
                currentState = currentState.updateEntity(source.entityId) { c ->
                    c.with(TappedComponent)
                }
                events.add(TappedEvent(source.entityId, source.name))
            }

            for ((_, production) in solution.manaProduced) {
                currentPool = if (production.color != null) {
                    currentPool.add(production.color)
                } else {
                    currentPool.addColorless(production.colorless)
                }
            }
        }

        // Deduct the cost from the pool
        val newPool = currentPool.pay(manaCost)
            ?: return executePayOrSufferConsequence(state, continuation, checkForMore)

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                )
            )
        }

        return checkForMore(currentState, events)
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
            pipeline = PipelineState(namedTargets = continuation.namedTargets)
        )

        // Execute the suffer effect using the registry
        val result = services.effectExecutorRegistry.execute(state, sufferEffect, context)

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
            var newState = state
            val events = mutableListOf<GameEvent>()

            events.add(PermanentsSacrificedEvent(playerId, selectedPermanents))

            for (permanentId in selectedPermanents) {
                val transitionResult = ZoneTransitionService.moveToZone(
                    newState, permanentId, Zone.GRAVEYARD
                )
                newState = transitionResult.state
                events.addAll(transitionResult.events)
            }

            // Now execute the consequence (e.g., sacrifice the source)
            val context = EffectContext(
                sourceId = continuation.sourceId,
                controllerId = continuation.controllerId,
                opponentId = null
            )
            val consequenceResult = services.effectExecutorRegistry.execute(newState, continuation.consequence, context)
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
        val projected = state.projectedState

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
        // Handle stun counters: remove a stun counter instead of untapping (CR 122.1b)
        for (entityId in toUntap) {
            val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            val stunCounters = newState.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
            if (stunCounters > 0) {
                newState = newState.updateEntity(entityId) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(counters.withRemoved(CounterType.STUN, 1))
                }
            } else {
                newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
                events.add(UntappedEvent(entityId, cardName))
            }
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
        val projected = newState.projectedState
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
