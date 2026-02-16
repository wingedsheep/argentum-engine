package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.SearchTargetLibraryExileEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SearchTargetLibraryExileEffect.
 *
 * "Search target player's library for up to N cards and exile them.
 * Then that player shuffles."
 *
 * Used for Supreme Inquisitor.
 *
 * This executor handles:
 * 1. Resolving the target player
 * 2. Getting all cards from the target player's library
 * 3. Creating a SearchLibraryDecision for the controller to choose up to N cards
 * 4. Pushing a SearchTargetLibraryExileContinuation to resume after selection
 */
class SearchTargetLibraryExileExecutor : EffectExecutor<SearchTargetLibraryExileEffect> {

    override val effectType: KClass<SearchTargetLibraryExileEffect> = SearchTargetLibraryExileEffect::class

    override fun execute(
        state: GameState,
        effect: SearchTargetLibraryExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for SearchTargetLibraryExile")

        val controllerId = context.controllerId
        val libraryZone = ZoneKey(targetPlayerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // If library is empty, just shuffle and return
        if (library.isEmpty()) {
            val shuffledLibrary = library.shuffled()
            val newState = state.copy(zones = state.zones + (libraryZone to shuffledLibrary))
            return ExecutionResult.success(
                newState,
                listOf(LibraryShuffledEvent(targetPlayerId))
            )
        }

        // Build card info map for the UI - ALL library cards are visible to controller
        val cardInfoMap = library.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = cardComponent?.imageUri
            )
        }

        val maxSelections = minOf(effect.count, library.size)

        // Create the decision for the controller to pick cards to exile
        val decisionId = UUID.randomUUID().toString()
        val decision = SearchLibraryDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Search target player's library for up to $maxSelections cards to exile",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = library,
            minSelections = 0, // Can "fail to find"
            maxSelections = maxSelections,
            cards = cardInfoMap,
            filterDescription = "card"
        )

        // Create continuation to resume after controller selects
        val continuation = SearchTargetLibraryExileContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            targetPlayerId = targetPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SEARCH_LIBRARY",
                    prompt = decision.prompt
                )
            )
        )
    }
}
