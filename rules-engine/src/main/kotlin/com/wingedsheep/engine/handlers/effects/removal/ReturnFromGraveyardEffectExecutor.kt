package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ReturnFromGraveyardEffect.
 * "Return target creature card from your graveyard to your hand"
 * "Return target creature card from a graveyard to the battlefield under your control"
 *
 * Handles returning cards from graveyard to:
 * - Hand (Gravedigger, Raise Dead)
 * - Battlefield (Breath of Life, Reanimate)
 *
 * Two modes:
 * 1. Pre-targeted (Gravedigger ETB): context.targets has the chosen card
 * 2. Decision-based (Elven Cache, Déjà Vu): no targets, shows card selection overlay
 */
class ReturnFromGraveyardEffectExecutor : EffectExecutor<ReturnFromGraveyardEffect> {

    override val effectType: KClass<ReturnFromGraveyardEffect> = ReturnFromGraveyardEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ReturnFromGraveyardEffect,
        context: EffectContext
    ): ExecutionResult {
        val target = context.targets.firstOrNull()

        // If no target provided, use decision-based flow
        if (target == null) {
            return executeWithDecision(state, effect, context)
        }

        // Pre-targeted flow (e.g. Gravedigger ETB)
        val (cardId, ownerId) = when (target) {
            is ChosenTarget.Card -> target.cardId to target.ownerId
            is ChosenTarget.Permanent -> target.entityId to context.controllerId
            else -> return ExecutionResult.error(state, "Invalid target type for return from graveyard")
        }

        val container = state.getEntity(cardId)
            ?: return ExecutionResult.error(state, "Card not found: $cardId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $cardId")

        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) {
            return ExecutionResult.error(state, "Card not in graveyard")
        }

        return when (effect.destination) {
            SearchDestination.HAND -> moveToHand(state, cardId, cardComponent, ownerId, context.controllerId)
            SearchDestination.BATTLEFIELD -> moveToBattlefield(state, cardId, cardComponent, ownerId, context.controllerId)
            else -> ExecutionResult.error(state, "Unsupported destination: ${effect.destination}")
        }
    }

    /**
     * Decision-based flow: show graveyard cards to the player for selection.
     */
    private fun executeWithDecision(
        state: GameState,
        effect: ReturnFromGraveyardEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)
        val graveyard = state.getZone(graveyardZone)

        // Filter cards matching the criteria
        val predicateContext = PredicateContext.fromEffectContext(context)
        val matchingCards = graveyard.filter { cardId ->
            predicateEvaluator.matches(state, cardId, effect.filter, predicateContext)
        }

        // No matches — spell resolves with no effect
        if (matchingCards.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Build card info map for the UI
        val cardInfoMap = matchingCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val filterDescription = effect.filter.description

        val decision = SearchLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose a $filterDescription from your graveyard to return to your hand",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = matchingCards,
            minSelections = 1,
            maxSelections = 1,
            cards = cardInfoMap,
            filterDescription = filterDescription
        )

        val continuation = ReturnFromGraveyardContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            destination = effect.destination,
            filter = effect.filter
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
                    decisionType = "SEARCH_GRAVEYARD",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun moveToHand(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        controllerId: EntityId
    ): ExecutionResult {
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        val handZone = ZoneKey(ownerId, ZoneType.HAND)

        var newState = state.removeFromZone(graveyardZone, cardId)
        newState = newState.addToZone(handZone, cardId)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    cardId,
                    cardComponent.name,
                    ZoneType.GRAVEYARD,
                    ZoneType.HAND,
                    ownerId
                )
            )
        )
    }

    private fun moveToBattlefield(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        controllerId: EntityId
    ): ExecutionResult {
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)

        var newState = state.removeFromZone(graveyardZone, cardId)
        newState = newState.addToZone(battlefieldZone, cardId)

        newState = newState.updateEntity(cardId) { c ->
            c.with(ControllerComponent(controllerId))
                .with(SummoningSicknessComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    cardId,
                    cardComponent.name,
                    ZoneType.GRAVEYARD,
                    ZoneType.BATTLEFIELD,
                    controllerId
                )
            )
        )
    }
}
