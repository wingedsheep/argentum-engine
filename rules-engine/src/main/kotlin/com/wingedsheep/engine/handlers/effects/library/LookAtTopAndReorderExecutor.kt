package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for LookAtTopAndReorderEffect.
 * "Look at the top N cards of your library and put them back in any order"
 *
 * This executor handles library manipulation by:
 * 1. Getting the top N cards from the target player's library
 * 2. Creating a ReorderLibraryDecision with embedded card info
 * 3. Pushing a ReorderLibraryContinuation to resume after player orders cards
 *
 * Special cases:
 * - Empty library: Return success immediately (nothing to look at)
 * - Fewer cards than N: Show all available cards
 */
class LookAtTopAndReorderExecutor : EffectExecutor<LookAtTopAndReorderEffect> {

    override val effectType: KClass<LookAtTopAndReorderEffect> = LookAtTopAndReorderEffect::class

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: LookAtTopAndReorderEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve target player (usually controller)
        val playerId = when (effect.target) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.PlayerRef -> EffectExecutorUtils.resolvePlayerTarget(effect.target, context)
                ?: return ExecutionResult.error(state, "No player found for LookAtTopAndReorder effect")
            else -> context.controllerId
        }

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // Evaluate the dynamic count
        val dynamicCount = dynamicAmountEvaluator.evaluate(state, effect.count, context)

        // If library is empty or count is zero, nothing to look at
        if (library.isEmpty() || dynamicCount <= 0) {
            return ExecutionResult.success(state)
        }

        // Get top N cards (or fewer if library is smaller)
        val count = minOf(dynamicCount, library.size)
        val topCards = library.take(count)

        // If only 1 card, no reordering needed - player sees it and it stays on top
        if (topCards.size == 1) {
            // Just emit a "looked at cards" event and continue
            return ExecutionResult.success(
                state,
                listOf(
                    LookedAtCardsEvent(
                        playerId = playerId,
                        cardIds = topCards,
                        source = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
                    )
                )
            )
        }

        // Build card info map for the UI
        val cardInfoMap = topCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        // Create the decision
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decision = ReorderLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Look at the top $count cards of your library. Put them back in any order.",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = topCards,
            cardInfo = cardInfoMap
        )

        // Create continuation to resume after player orders cards
        val continuation = ReorderLibraryContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        // Push continuation and return paused state
        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "REORDER_LIBRARY",
                    prompt = decision.prompt
                )
            )
        )
    }
}
