package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.DrawReplacementBounceContinuation
import com.wingedsheep.engine.core.DrawReplacementDiscardContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import kotlin.reflect.KClass

/**
 * Executor for DrawCardsEffect.
 * "Draw X cards" or "Target player draws X cards"
 */
class DrawCardsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
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

            // Check for bear token replacement shields (Words of Wilding)
            val bearTokenResult = consumeBearTokenReplacementShield(newState, playerId)
            if (bearTokenResult != null) {
                newState = bearTokenResult.first
                events.addAll(bearTokenResult.second)
                continue
            }

            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) {
                // Failed to draw - game loss condition (Rule 704.5c)
                newState = newState.updateEntity(playerId) { container ->
                    container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                }
                events.add(DrawFailedEvent(playerId, "Empty library"))
                if (drawnCards.isNotEmpty()) {
                    events.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList()))
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
            events.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards))
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
     * Checks for and consumes a bear token draw replacement shield (Words of Wilding).
     * Returns the updated state and events if a shield was consumed, or null if no shield exists.
     */
    private fun consumeBearTokenReplacementShield(
        state: GameState,
        playerId: EntityId
    ): Pair<GameState, List<GameEvent>>? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithBearToken &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // Create a 2/2 green Bear creature token instead of drawing
        newState = createBearToken(newState, playerId)

        return newState to emptyList()
    }

    /**
     * Checks for and consumes a bounce draw replacement shield (Words of Wind).
     * If found, pauses execution to ask each player to choose a permanent to bounce.
     * Returns a paused ExecutionResult if a shield was consumed, or null if no shield exists.
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

        val shield = state.floatingEffects[shieldIndex]

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // Emit CardsDrawnEvent for cards drawn before this shield was hit
        val allEvents = eventsSoFar.toMutableList()
        if (drawnCardsSoFar.isNotEmpty()) {
            allEvents.add(0, CardsDrawnEvent(playerId, drawnCardsSoFar.size, drawnCardsSoFar))
        }

        // Get players in APNAP order for the bounce
        val activePlayer = newState.activePlayerId ?: return null
        val apnapOrder = listOf(activePlayer) + newState.turnOrder.filter { it != activePlayer }

        // Find first player with permanents to bounce
        val projected = stateProjector.project(newState)
        val playersWithPermanents = apnapOrder.filter { pid ->
            projected.getBattlefieldControlledBy(pid).isNotEmpty()
        }

        if (playersWithPermanents.isEmpty()) {
            // No player has permanents - draw is still replaced (no card drawn), just continue
            return ExecutionResult.success(newState, allEvents)
                .let { result ->
                    // If there are remaining draws, continue processing them
                    if (remainingDraws > 0) {
                        val drawResult = executeDraws(result.state, playerId, remainingDraws)
                        ExecutionResult(
                            drawResult.state,
                            allEvents + drawResult.events,
                            drawResult.error
                        )
                    } else {
                        ExecutionResult.success(result.state, allEvents)
                    }
                }
        }

        val firstPlayer = playersWithPermanents.first()
        val remainingBounce = playersWithPermanents.drop(1)

        val controlledPermanents = projected.getBattlefieldControlledBy(firstPlayer)

        // If the player has exactly 1 permanent, auto-select it
        if (controlledPermanents.size == 1) {
            val permanentId = controlledPermanents.first()
            val bounceResult = EffectExecutorUtils.moveCardToZone(newState, permanentId, Zone.HAND)
            newState = bounceResult.state
            allEvents.addAll(bounceResult.events)

            // Continue with remaining players
            return continueBounceChain(
                newState, remainingBounce, playerId, remainingDraws,
                drawnCardsSoFar, allEvents, shield.sourceId, shield.sourceName
            )
        }

        // Present decision to first player
        val sourceName = shield.sourceName ?: "Words of Wind"
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = newState,
            playerId = firstPlayer,
            sourceId = shield.sourceId,
            sourceName = sourceName,
            prompt = "Choose a permanent to return to its owner's hand",
            options = controlledPermanents,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = DrawReplacementBounceContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            drawingPlayerId = playerId,
            currentBouncingPlayerId = firstPlayer,
            remainingPlayers = remainingBounce,
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
            allEvents.add(0, CardsDrawnEvent(playerId, drawnCardsSoFar.size, drawnCardsSoFar))
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
                allEvents.add(CardsDiscardedEvent(opponentId, listOf(cardId)))
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
     * Continue the bounce chain for remaining players after a player auto-bounced.
     */
    private fun continueBounceChain(
        state: GameState,
        remainingPlayers: List<EntityId>,
        drawingPlayerId: EntityId,
        remainingDraws: Int,
        drawnCardsSoFar: List<EntityId>,
        events: MutableList<GameEvent>,
        sourceId: EntityId?,
        sourceName: String?
    ): ExecutionResult {
        if (remainingPlayers.isEmpty()) {
            // All players done - continue with remaining draws
            if (remainingDraws > 0) {
                val drawResult = executeDraws(state, drawingPlayerId, remainingDraws)
                return ExecutionResult(
                    drawResult.state,
                    events + drawResult.events,
                    drawResult.error
                )
            }
            return ExecutionResult.success(state, events)
        }

        val projected = stateProjector.project(state)
        val nextPlayer = remainingPlayers.first()
        val nextRemaining = remainingPlayers.drop(1)
        val controlledPermanents = projected.getBattlefieldControlledBy(nextPlayer)

        if (controlledPermanents.isEmpty()) {
            // Skip player with no permanents
            return continueBounceChain(
                state, nextRemaining, drawingPlayerId, remainingDraws,
                drawnCardsSoFar, events, sourceId, sourceName
            )
        }

        if (controlledPermanents.size == 1) {
            // Auto-bounce single permanent
            val permanentId = controlledPermanents.first()
            val bounceResult = EffectExecutorUtils.moveCardToZone(state, permanentId, Zone.HAND)
            events.addAll(bounceResult.events)
            return continueBounceChain(
                bounceResult.state, nextRemaining, drawingPlayerId, remainingDraws,
                drawnCardsSoFar, events, sourceId, sourceName
            )
        }

        // Present decision
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = nextPlayer,
            sourceId = sourceId,
            sourceName = sourceName ?: "Words of Wind",
            prompt = "Choose a permanent to return to its owner's hand",
            options = controlledPermanents,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = DrawReplacementBounceContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            drawingPlayerId = drawingPlayerId,
            currentBouncingPlayerId = nextPlayer,
            remainingPlayers = nextRemaining,
            remainingDraws = remainingDraws,
            drawnCardsSoFar = drawnCardsSoFar,
            sourceId = sourceId,
            sourceName = sourceName
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            events + decisionResult.events
        )
    }

    companion object {
        /**
         * Create a 2/2 green Bear creature token for the given player.
         * Used by Words of Wilding draw replacement.
         */
        fun createBearToken(state: GameState, playerId: EntityId): GameState {
            val tokenId = EntityId.generate()
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:Bear",
                name = "Bear",
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Creature - Bear"),
                baseStats = CreatureStats(2, 2),
                baseKeywords = emptySet(),
                colors = setOf(Color.GREEN),
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
