package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EachPlayerDrawsXEffect
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerDrawsXEffect.
 * "Each player draws X cards" (Prosperity)
 *
 * The X value comes from the effect context (from the spell's xValue when cast).
 */
class EachPlayerDrawsXExecutor : EffectExecutor<EachPlayerDrawsXEffect> {

    override val effectType: KClass<EachPlayerDrawsXEffect> = EachPlayerDrawsXEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerDrawsXEffect,
        context: EffectContext
    ): ExecutionResult {
        val xValue = context.xValue ?: 0

        if (xValue == 0) {
            // X=0 means no cards are drawn
            return ExecutionResult.success(state, emptyList())
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Get all players that should draw
        val playersToDraw = mutableListOf<EntityId>()

        if (effect.includeController) {
            playersToDraw.add(context.controllerId)
        }

        if (effect.includeOpponents) {
            context.opponentId?.let { playersToDraw.add(it) }
        }

        // Each player draws X cards
        for (playerId in playersToDraw) {
            val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
            val handZone = ZoneKey(playerId, ZoneType.HAND)
            val drawnCards = mutableListOf<EntityId>()

            repeat(xValue) {
                val library = newState.getZone(libraryZone)
                if (library.isEmpty()) {
                    // Failed to draw - this will trigger a game loss check
                    events.add(DrawFailedEvent(playerId, "Empty library"))
                    return@repeat
                }

                // Draw from top of library (first card)
                val cardId = library.first()
                drawnCards.add(cardId)

                newState = newState.removeFromZone(libraryZone, cardId)
                newState = newState.addToZone(handZone, cardId)
            }

            if (drawnCards.isNotEmpty()) {
                events.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
