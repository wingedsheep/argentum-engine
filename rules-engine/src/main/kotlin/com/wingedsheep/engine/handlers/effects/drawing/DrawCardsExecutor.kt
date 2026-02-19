package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.DrawReplacementActivationContinuation
import com.wingedsheep.engine.core.DrawReplacementDiscardContinuation
import com.wingedsheep.engine.core.DrawReplacementRemainingDrawsContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.Effect
import kotlin.reflect.KClass

/**
 * Executor for DrawCardsEffect.
 * "Draw X cards" or "Target player draws X cards"
 */
class DrawCardsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: CardRegistry? = null,
    private val effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null
) : EffectExecutor<DrawCardsEffect> {

    override val effectType: KClass<DrawCardsEffect> = DrawCardsEffect::class

    private val decisionHandler = DecisionHandler()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = EffectExecutorUtils.resolvePlayerTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid player for draw")

        val count = amountEvaluator.evaluate(state, effect.count, context)
        return executeDraws(state, playerId, count)
    }

    /**
     * Execute a sequence of draws, checking for replacement shields on each draw.
     * This is also called from ContinuationHandler when resuming remaining draws
     * after a bounce replacement.
     */
    fun executeDraws(
        state: GameState,
        playerId: EntityId,
        count: Int
    ): ExecutionResult {
        var newState = state
        val drawnCards = mutableListOf<EntityId>()
        val events = mutableListOf<GameEvent>()

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)

        for (i in 0 until count) {
            // Check for life gain replacement shields (Words of Worship)
            val lifeGainResult = consumeLifeGainReplacementShield(newState, playerId)
            if (lifeGainResult != null) {
                newState = lifeGainResult.first
                events.addAll(lifeGainResult.second)
                continue
            }

            // Check for damage replacement shields (Words of War)
            val damageResult = consumeDamageReplacementShield(newState, playerId)
            if (damageResult != null) {
                newState = damageResult.first
                events.addAll(damageResult.second)
                continue
            }

            // Check for bounce replacement shields (Words of Wind)
            val bounceResult = consumeBounceReplacementShield(
                newState, playerId, count - i - 1, drawnCards.toList(), events
            )
            if (bounceResult != null) {
                return bounceResult
            }

            // Check for discard replacement shields (Words of Waste)
            val discardResult = consumeDiscardReplacementShield(
                newState, playerId, count - i - 1, drawnCards.toList(), events
            )
            if (discardResult != null) {
                return discardResult
            }

            // Check for token replacement shields (Words of Wilding)
            val tokenResult = consumeTokenReplacementShield(newState, playerId)
            if (tokenResult != null) {
                newState = tokenResult.first
                events.addAll(tokenResult.second)
                continue
            }

            // No shield exists - check if player wants to activate a promptOnDraw ability
            val promptResult = checkPromptOnDraw(
                newState, playerId, count - i, drawnCards.toList()
            )
            if (promptResult != null) {
                // Emit CardsDrawnEvent for cards drawn before this prompt
                if (drawnCards.isNotEmpty()) {
                    val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                    val allEvents = mutableListOf<GameEvent>(
                        CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames)
                    )
                    allEvents.addAll(events)
                    allEvents.addAll(promptResult.events)
                    return ExecutionResult.paused(
                        promptResult.state,
                        promptResult.pendingDecision!!,
                        allEvents
                    )
                }
                return promptResult
            }

            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) {
                // Failed to draw - game loss condition (Rule 704.5c)
                newState = newState.updateEntity(playerId) { container ->
                    container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                }
                events.add(DrawFailedEvent(playerId, "Empty library"))
                if (drawnCards.isNotEmpty()) {
                    val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                    events.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames))
                }
                return ExecutionResult.success(newState, events)
            }

            // Draw from top of library (first card)
            val cardId = library.first()
            drawnCards.add(cardId)

            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)
        }

        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Checks for and consumes a life gain draw replacement shield (Words of Worship).
     * Returns the updated state and events if a shield was consumed, or null if no shield exists.
     */
    private fun consumeLifeGainReplacementShield(
        state: GameState,
        playerId: EntityId
    ): Pair<GameState, List<GameEvent>>? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithLifeGain &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val shield = state.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.ReplaceDrawWithLifeGain

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // Apply life gain instead of drawing
        val currentLife = newState.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: return null
        val newLife = currentLife + mod.lifeAmount
        newState = newState.updateEntity(playerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return newState to listOf(
            LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN)
        )
    }

    /**
     * Checks for and consumes a damage draw replacement shield (Words of War).
     * Returns the updated state and events if a shield was consumed, or null if no shield exists.
     */
    private fun consumeDamageReplacementShield(
        state: GameState,
        playerId: EntityId
    ): Pair<GameState, List<GameEvent>>? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithDamage &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val shield = state.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.ReplaceDrawWithDamage

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        val newState = state.copy(floatingEffects = updatedEffects)

        // Deal damage to the chosen target instead of drawing
        val damageResult = EffectExecutorUtils.dealDamageToTarget(
            newState, mod.targetId, mod.damageAmount, shield.sourceId
        )

        return damageResult.state to damageResult.events
    }

    /**
     * Checks for and consumes a token draw replacement shield (Words of Wilding).
     * Returns the updated state and events if a shield was consumed, or null if no shield exists.
     */
    private fun consumeTokenReplacementShield(
        state: GameState,
        playerId: EntityId
    ): Pair<GameState, List<GameEvent>>? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithToken &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val mod = state.floatingEffects[shieldIndex].effect.modification as SerializableModification.ReplaceDrawWithToken

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // Create the token instead of drawing
        newState = createToken(newState, playerId, mod)

        return newState to emptyList()
    }

    /**
     * Checks for and consumes a bounce draw replacement shield (Words of Wind).
     * Executes the bounce via the atomic ForEachPlayer pipeline.
     * Returns an ExecutionResult if a shield was consumed, or null if no shield exists.
     */
    private fun consumeBounceReplacementShield(
        state: GameState,
        playerId: EntityId,
        remainingDraws: Int,
        drawnCardsSoFar: List<EntityId>,
        eventsSoFar: List<GameEvent>
    ): ExecutionResult? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithBounce &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val executor = effectExecutor ?: return null

        val shield = state.floatingEffects[shieldIndex]

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // Emit CardsDrawnEvent for cards drawn before this shield was hit
        val allEvents = eventsSoFar.toMutableList()
        if (drawnCardsSoFar.isNotEmpty()) {
            val cardNames = drawnCardsSoFar.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            allEvents.add(0, CardsDrawnEvent(playerId, drawnCardsSoFar.size, drawnCardsSoFar, cardNames))
        }

        // If there are remaining draws, push a continuation so they resume after the pipeline
        if (remainingDraws > 0) {
            val remainingDrawsContinuation = DrawReplacementRemainingDrawsContinuation(
                drawingPlayerId = playerId,
                remainingDraws = remainingDraws,
                isDrawStep = false
            )
            newState = newState.pushContinuation(remainingDrawsContinuation)
        }

        // Build and execute the bounce pipeline
        val pipeline = EffectPatterns.eachPlayerReturnsPermanentToHand()
        val context = EffectContext(
            controllerId = playerId,
            sourceId = shield.sourceId,
            opponentId = null
        )

        val pipelineResult = executor(newState, pipeline, context)

        if (pipelineResult.isPaused) {
            // Pipeline needs a decision — remaining-draws continuation is underneath
            return ExecutionResult.paused(
                pipelineResult.state,
                pipelineResult.pendingDecision!!,
                allEvents + pipelineResult.events
            )
        }

        // Pipeline completed synchronously
        var resultState = pipelineResult.state
        val resultEvents = allEvents + pipelineResult.events

        // Pop the remaining-draws continuation if we pushed one (pipeline didn't use it)
        if (remainingDraws > 0) {
            val (popped, stateAfterPop) = resultState.popContinuation()
            if (popped is DrawReplacementRemainingDrawsContinuation) {
                resultState = stateAfterPop
                // Continue with remaining draws
                val drawResult = executeDraws(resultState, playerId, remainingDraws)
                return ExecutionResult(
                    drawResult.state,
                    resultEvents + drawResult.events,
                    drawResult.error
                )
            }
            // If the continuation was something else, put it back (shouldn't happen)
            resultState = pipelineResult.state
        }

        return ExecutionResult.success(resultState, resultEvents)
    }

    /**
     * Checks for and consumes a discard draw replacement shield (Words of Waste).
     * If found, each opponent discards a card instead of the draw.
     * Returns a paused ExecutionResult if an opponent must choose, or null if no shield exists.
     */
    private fun consumeDiscardReplacementShield(
        state: GameState,
        playerId: EntityId,
        remainingDraws: Int,
        drawnCardsSoFar: List<EntityId>,
        eventsSoFar: List<GameEvent>
    ): ExecutionResult? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithDiscard &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val shield = state.floatingEffects[shieldIndex]

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // Emit CardsDrawnEvent for cards drawn before this shield was hit
        val allEvents = eventsSoFar.toMutableList()
        if (drawnCardsSoFar.isNotEmpty()) {
            val cardNames = drawnCardsSoFar.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            allEvents.add(0, CardsDrawnEvent(playerId, drawnCardsSoFar.size, drawnCardsSoFar, cardNames))
        }

        // Get each opponent and make them discard
        val opponents = newState.turnOrder.filter { it != playerId }

        for (opponentId in opponents) {
            val handZone = ZoneKey(opponentId, Zone.HAND)
            val hand = newState.getZone(handZone)

            if (hand.isEmpty()) {
                // Opponent has no cards, skip
                continue
            }

            if (hand.size == 1) {
                // Auto-discard the single card
                val cardId = hand.first()
                val graveyardZone = ZoneKey(opponentId, Zone.GRAVEYARD)
                newState = newState.removeFromZone(handZone, cardId)
                newState = newState.addToZone(graveyardZone, cardId)
                val discardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                allEvents.add(CardsDiscardedEvent(opponentId, listOf(cardId), listOf(discardName)))
                continue
            }

            // Opponent must choose which card to discard - pause for decision
            val sourceName = shield.sourceName ?: "Words of Waste"
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = opponentId,
                sourceId = shield.sourceId,
                sourceName = sourceName,
                prompt = "Choose a card to discard",
                options = hand,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = DrawReplacementDiscardContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                drawingPlayerId = playerId,
                discardingPlayerId = opponentId,
                remainingDraws = remainingDraws,
                drawnCardsSoFar = drawnCardsSoFar,
                sourceId = shield.sourceId,
                sourceName = sourceName
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                allEvents + decisionResult.events
            )
        }

        // All opponents had 0-1 cards, handled inline - continue with remaining draws
        if (remainingDraws > 0) {
            val drawResult = executeDraws(newState, playerId, remainingDraws)
            return ExecutionResult(
                drawResult.state,
                allEvents + drawResult.events,
                drawResult.error
            )
        }
        return ExecutionResult.success(newState, allEvents)
    }

    /**
     * Check if a player has a "prompt on draw" activated ability that they can afford.
     * If so, present a mana source selection decision and pause.
     * Returns null if no prompt is needed (no cardRegistry, no ability, can't afford).
     *
     * This is the same logic as TurnManager.checkPromptOnDraw() but for spell/ability draws.
     */
    internal fun checkPromptOnDraw(
        state: GameState,
        playerId: EntityId,
        remainingDrawCount: Int,
        drawnCardsSoFar: List<EntityId>,
        declinedSourceIds: List<EntityId> = emptyList()
    ): ExecutionResult? {
        if (cardRegistry == null) return null

        val projected = stateProjector.project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(playerId)

        for (permanentId in controlledPermanents) {
            if (permanentId in declinedSourceIds) continue
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (ability in cardDef.script.activatedAbilities) {
                if (!ability.promptOnDraw) continue

                val manaCost = when (val cost = ability.cost) {
                    is com.wingedsheep.sdk.scripting.AbilityCost.Mana -> cost.cost
                    is com.wingedsheep.sdk.scripting.AbilityCost.Composite -> {
                        cost.costs.filterIsInstance<com.wingedsheep.sdk.scripting.AbilityCost.Mana>()
                            .firstOrNull()?.cost
                    }
                    else -> null
                } ?: continue

                val manaSolver = ManaSolver(cardRegistry, stateProjector)
                if (!manaSolver.canPay(state, playerId, manaCost)) continue

                val sources = manaSolver.findAvailableManaSources(state, playerId)
                val sourceOptions = sources.map { source ->
                    ManaSourceOption(
                        entityId = source.entityId,
                        name = source.name,
                        producesColors = source.producesColors,
                        producesColorless = source.producesColorless
                    )
                }

                val solution = manaSolver.solve(state, playerId, manaCost)
                val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

                val decisionId = java.util.UUID.randomUUID().toString()
                val decision = SelectManaSourcesDecision(
                    id = decisionId,
                    playerId = playerId,
                    prompt = "Pay ${manaCost} to activate ${card.name}?",
                    context = DecisionContext(
                        sourceId = permanentId,
                        sourceName = card.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    availableSources = sourceOptions,
                    requiredCost = manaCost.toString(),
                    autoPaySuggestion = autoPaySuggestion,
                    canDecline = true
                )

                val continuation = DrawReplacementActivationContinuation(
                    decisionId = decisionId,
                    drawingPlayerId = playerId,
                    sourceId = permanentId,
                    sourceName = card.name,
                    abilityEffect = ability.effect,
                    manaCost = manaCost.toString(),
                    drawCount = remainingDrawCount,
                    isDrawStep = false,
                    drawnCardsSoFar = drawnCardsSoFar,
                    targetRequirements = ability.targetRequirements,
                    declinedSourceIds = declinedSourceIds
                )

                val stateWithDecision = state.withPendingDecision(decision)
                val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

                return ExecutionResult.paused(
                    stateWithContinuation,
                    decision,
                    listOf(
                        DecisionRequestedEvent(
                            decisionId = decisionId,
                            playerId = playerId,
                            decisionType = "SELECT_MANA_SOURCES",
                            prompt = decision.prompt
                        )
                    )
                )
            }
        }

        return null
    }

    companion object {
        /**
         * Create a creature token for the given player using a [SerializableModification.ReplaceDrawWithToken] spec.
         * Used by Words of Wilding draw replacement.
         */
        fun createToken(
            state: GameState,
            playerId: EntityId,
            spec: SerializableModification.ReplaceDrawWithToken
        ): GameState {
            val tokenId = EntityId.generate()
            val creatureTypesStr = spec.creatureTypes.joinToString(" ")
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:${spec.creatureTypes.joinToString("-")}",
                name = creatureTypesStr,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Creature - $creatureTypesStr"),
                baseStats = CreatureStats(spec.power, spec.toughness),
                baseKeywords = emptySet(),
                colors = spec.colors,
                ownerId = playerId
            )

            val container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(playerId),
                SummoningSicknessComponent
            )

            var newState = state.withEntity(tokenId, container)
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)

            return newState
        }
    }
}
