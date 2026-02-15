package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Zone
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

    override fun execute(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = EffectExecutorUtils.resolvePlayerTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid player for draw")

        var newState = state
        val drawnCards = mutableListOf<EntityId>()
        val events = mutableListOf<GameEvent>()

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)
        val count = amountEvaluator.evaluate(state, effect.count, context)

        repeat(count) {
            // Check for draw replacement shields (e.g., Words of Worship)
            val replacementResult = consumeDrawReplacementShield(newState, playerId)
            if (replacementResult != null) {
                newState = replacementResult.first
                events.addAll(replacementResult.second)
                return@repeat
            }

            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) {
                // Failed to draw - game loss condition (Rule 704.5c)
                // Mark player as having lost due to empty library
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
     * Checks for and consumes a draw replacement shield (e.g., Words of Worship).
     * Returns the updated state and events if a shield was consumed, or null if no shield exists.
     */
    private fun consumeDrawReplacementShield(
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
}
