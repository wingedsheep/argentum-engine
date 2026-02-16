package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.SelectionMode
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SelectFromCollectionEffect.
 *
 * Reads cards from a named collection in [EffectContext.storedCollections],
 * presents a selection decision to the player (or auto-selects), and stores
 * the selected and remainder collections.
 *
 * When a player decision is needed, pushes a [SelectFromCollectionContinuation]
 * and returns paused. The continuation handler splits the collections when
 * the player responds.
 */
class SelectFromCollectionExecutor : EffectExecutor<SelectFromCollectionEffect> {

    override val effectType: KClass<SelectFromCollectionEffect> = SelectFromCollectionEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: SelectFromCollectionEffect,
        context: EffectContext
    ): ExecutionResult {
        val cards = context.storedCollections[effect.from]
            ?: return ExecutionResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val remainderName = effect.storeRemainder

        if (cards.isEmpty()) {
            // Nothing to select from — store empty collections
            val collections = mutableMapOf(effect.storeSelected to emptyList<EntityId>())
            if (remainderName != null) {
                collections[remainderName] = emptyList()
            }
            return ExecutionResult.success(state).copy(updatedCollections = collections)
        }

        return when (val selection = effect.selection) {
            is SelectionMode.All -> {
                // Auto-select all, no decision needed
                val collections = mutableMapOf(effect.storeSelected to cards)
                if (remainderName != null) {
                    collections[remainderName] = emptyList()
                }
                ExecutionResult.success(state).copy(updatedCollections = collections)
            }

            is SelectionMode.ChooseExactly -> {
                val count = amountEvaluator.evaluate(state, selection.count, context)
                if (count >= cards.size) {
                    // Must select all — no choice needed
                    val collections = mutableMapOf(effect.storeSelected to cards)
                    if (remainderName != null) {
                        collections[remainderName] = emptyList()
                    }
                    ExecutionResult.success(state).copy(updatedCollections = collections)
                } else {
                    createDecision(state, context, effect, cards, minOf(count, cards.size), minOf(count, cards.size))
                }
            }

            is SelectionMode.ChooseUpTo -> {
                val count = amountEvaluator.evaluate(state, selection.count, context)
                createDecision(state, context, effect, cards, 0, minOf(count, cards.size))
            }

            is SelectionMode.OpponentChooses -> {
                val count = amountEvaluator.evaluate(state, selection.count, context)
                val opponentId = context.opponentId
                    ?: return ExecutionResult.error(state, "No opponent for OpponentChooses selection mode")
                createDecision(state, context, effect, cards, minOf(count, cards.size), minOf(count, cards.size), opponentId)
            }
        }
    }

    private fun createDecision(
        state: GameState,
        context: EffectContext,
        effect: SelectFromCollectionEffect,
        cards: List<EntityId>,
        minSelections: Int,
        maxSelections: Int,
        decidingPlayerId: EntityId? = null
    ): ExecutionResult {
        val playerId = decidingPlayerId ?: context.controllerId
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Build card info for hidden-zone cards (library cards are normally hidden)
        val cardInfoMap = cards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val prompt = when {
            minSelections == maxSelections -> "Choose $minSelections card${if (minSelections != 1) "s" else ""}"
            minSelections == 0 -> "Choose up to $maxSelections card${if (maxSelections != 1) "s" else ""}"
            else -> "Choose $minSelections to $maxSelections cards"
        }

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = cards,
            minSelections = minSelections,
            maxSelections = maxSelections,
            ordered = false,
            cardInfo = cardInfoMap
        )

        val continuation = SelectFromCollectionContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            allCards = cards,
            storeSelected = effect.storeSelected,
            storeRemainder = effect.storeRemainder,
            storedCollections = context.storedCollections
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
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }
}
