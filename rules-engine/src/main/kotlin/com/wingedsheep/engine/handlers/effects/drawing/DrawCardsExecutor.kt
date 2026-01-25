package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DrawCardsEffect

/**
 * Executor for DrawCardsEffect.
 * "Draw X cards" or "Target player draws X cards"
 */
class DrawCardsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DrawCardsEffect> {

    override fun execute(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for draw")

        var newState = state
        val drawnCards = mutableListOf<EntityId>()

        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val count = amountEvaluator.evaluate(state, effect.count, context)

        repeat(count) {
            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) {
                // Failed to draw - game loss condition
                return ExecutionResult.success(
                    newState,
                    listOf(DrawFailedEvent(playerId, "Empty library"))
                )
            }

            // Draw from top of library (first card)
            val cardId = library.first()
            drawnCards.add(cardId)

            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDrawnEvent(playerId, drawnCards.size, drawnCards))
        )
    }
}
