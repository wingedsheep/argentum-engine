package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.toEntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RevealUntilNonlandDealDamageEachTargetEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for RevealUntilNonlandDealDamageEachTargetEffect.
 *
 * "Choose any number of target players or planeswalkers. For each of them,
 * reveal cards from the top of your library until you reveal a nonland card,
 * Kaboom! deals damage equal to that card's mana value to that player or
 * planeswalker, then you put the revealed cards on the bottom of your library
 * in any order."
 *
 * Processes targets one at a time. If a reorder decision is needed (>1 card
 * revealed), pauses with a KaboomReorderContinuation that stores the remaining
 * targets. The continuation handler resumes by processing the next target.
 */
class RevealUntilNonlandDealDamageEachTargetExecutor : EffectExecutor<RevealUntilNonlandDealDamageEachTargetEffect> {

    override val effectType: KClass<RevealUntilNonlandDealDamageEachTargetEffect> =
        RevealUntilNonlandDealDamageEachTargetEffect::class

    override fun execute(
        state: GameState,
        effect: RevealUntilNonlandDealDamageEachTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetIds = context.targets.map { it.toEntityId() }

        if (targetIds.isEmpty()) {
            return ExecutionResult.success(state)
        }

        return processTargets(state, context.controllerId, context.sourceId, targetIds)
    }

    companion object {
        /**
         * Process targets starting from the first one in the list.
         * This is called both from the executor and from the continuation handler.
         */
        fun processTargets(
            state: GameState,
            controllerId: EntityId,
            sourceId: EntityId?,
            targetIds: List<EntityId>
        ): ExecutionResult {
            var currentState = state
            val allEvents = mutableListOf<GameEvent>()
            val sourceName = sourceId?.let { currentState.getEntity(it)?.get<CardComponent>()?.name }

            for ((index, targetId) in targetIds.withIndex()) {
                val result = processOneTarget(currentState, controllerId, sourceId, sourceName, targetId)
                currentState = result.state
                allEvents.addAll(result.events)

                if (result.needsReorder) {
                    // Pause for reorder decision with remaining targets
                    val remainingTargets = targetIds.drop(index + 1)
                    return pauseForReorder(
                        currentState, controllerId, sourceId, sourceName,
                        result.revealedCards, remainingTargets, allEvents
                    )
                }
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        private data class SingleTargetResult(
            val state: GameState,
            val events: List<GameEvent>,
            val revealedCards: List<EntityId>,
            val needsReorder: Boolean
        )

        private fun processOneTarget(
            state: GameState,
            controllerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            targetId: EntityId
        ): SingleTargetResult {
            val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
            val library = state.getZone(libraryZone)

            if (library.isEmpty()) {
                return SingleTargetResult(state, emptyList(), emptyList(), false)
            }

            // Reveal cards from top until nonland
            val revealedCards = mutableListOf<EntityId>()
            var nonlandCard: EntityId? = null

            for (cardId in library) {
                val container = state.getEntity(cardId)
                val cardComponent = container?.get<CardComponent>()
                revealedCards.add(cardId)

                if (cardComponent != null && !cardComponent.isLand) {
                    nonlandCard = cardId
                    break
                }
            }

            val events = mutableListOf<GameEvent>()

            // Emit reveal event
            val cardNames = revealedCards.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = revealedCards.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = controllerId,
                    cardIds = revealedCards.toList(),
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName
                )
            )

            // Deal damage if nonland found
            var currentState = state
            if (nonlandCard != null) {
                val manaValue = currentState.getEntity(nonlandCard)?.get<CardComponent>()?.manaValue ?: 0
                if (manaValue > 0) {
                    val damageResult = EffectExecutorUtils.dealDamageToTarget(
                        currentState, targetId, manaValue, sourceId
                    )
                    currentState = damageResult.state
                    events.addAll(damageResult.events)
                }
            }

            // Remove revealed cards from library
            val revealedSet = revealedCards.toSet()
            val remainingLibrary = currentState.getZone(libraryZone).filter { it !in revealedSet }
            currentState = currentState.copy(
                zones = currentState.zones + (libraryZone to remainingLibrary)
            )

            // Put revealed cards on bottom
            if (revealedCards.size <= 1) {
                if (revealedCards.isNotEmpty()) {
                    val newLibrary = remainingLibrary + revealedCards
                    currentState = currentState.copy(
                        zones = currentState.zones + (libraryZone to newLibrary)
                    )
                }
                return SingleTargetResult(currentState, events, revealedCards.toList(), false)
            }

            // Multiple cards: need reorder decision
            return SingleTargetResult(currentState, events, revealedCards.toList(), true)
        }

        private fun pauseForReorder(
            state: GameState,
            controllerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            revealedCards: List<EntityId>,
            remainingTargets: List<EntityId>,
            events: MutableList<GameEvent>
        ): ExecutionResult {
            val decisionId = UUID.randomUUID().toString()

            val cardInfoMap = revealedCards.associateWith { cardId ->
                val container = state.getEntity(cardId)
                val cardComp = container?.get<CardComponent>()
                SearchCardInfo(
                    name = cardComp?.name ?: "Unknown",
                    manaCost = cardComp?.manaCost?.toString() ?: "",
                    typeLine = cardComp?.typeLine?.toString() ?: "",
                    imageUri = cardComp?.imageUri
                )
            }

            val decision = ReorderLibraryDecision(
                id = decisionId,
                playerId = controllerId,
                prompt = "Put the revealed cards on the bottom of your library in any order.",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                cards = revealedCards.toList(),
                cardInfo = cardInfoMap
            )

            val continuation = KaboomReorderContinuation(
                decisionId = decisionId,
                playerId = controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingTargetIds = remainingTargets
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

            events.add(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "REORDER_LIBRARY",
                    prompt = decision.prompt
                )
            )

            return ExecutionResult.paused(stateWithContinuation, decision, events)
        }
    }
}
