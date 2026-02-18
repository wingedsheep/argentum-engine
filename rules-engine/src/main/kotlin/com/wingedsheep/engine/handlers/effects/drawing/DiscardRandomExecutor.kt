package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DiscardRandomEffect
import kotlin.reflect.KClass

/**
 * Executor for DiscardRandomEffect.
 * "Discard X cards at random" or "Target player/opponent discards X cards at random"
 *
 * Unlike DiscardCardsExecutor, this randomly selects which cards to discard
 * rather than asking the player to choose.
 */
class DiscardRandomExecutor : EffectExecutor<DiscardRandomEffect> {

    override val effectType: KClass<DiscardRandomEffect> = DiscardRandomEffect::class

    override fun execute(
        state: GameState,
        effect: DiscardRandomEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for random discard")

        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)

        if (hand.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Randomly select cards to discard (up to count or all if fewer)
        val cardsToDiscard = hand.shuffled().take(effect.count)

        return discardCards(state, playerId, cardsToDiscard)
    }

    /**
     * Actually discard the specified cards.
     */
    private fun discardCards(
        state: GameState,
        playerId: EntityId,
        cardIds: List<EntityId>
    ): ExecutionResult {
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in cardIds) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val cardNames = cardIds.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, cardIds, cardNames))
        )
    }
}
