package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ExileOpponentsGraveyardsEffect
import kotlin.reflect.KClass

/**
 * Executor for ExileOpponentsGraveyardsEffect.
 * Exiles all cards in each opponent's graveyard.
 */
class ExileOpponentsGraveyardsExecutor : EffectExecutor<ExileOpponentsGraveyardsEffect> {

    override val effectType: KClass<ExileOpponentsGraveyardsEffect> = ExileOpponentsGraveyardsEffect::class

    override fun execute(
        state: GameState,
        effect: ExileOpponentsGraveyardsEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val controllerId = context.controllerId

        // For each opponent, exile their entire graveyard
        for (playerId in state.turnOrder) {
            if (playerId == controllerId) continue

            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            val exileZone = ZoneKey(playerId, Zone.EXILE)
            val cardIds = newState.getZone(graveyardZone).toList()

            for (cardId in cardIds) {
                val cardComp = newState.getEntity(cardId)?.get<CardComponent>()
                val ownerId = cardComp?.ownerId ?: playerId
                val ownerExileZone = ZoneKey(ownerId, Zone.EXILE)

                newState = newState.removeFromZone(graveyardZone, cardId)
                newState = newState.addToZone(ownerExileZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        cardId,
                        cardComp?.name ?: "Unknown",
                        Zone.GRAVEYARD,
                        Zone.EXILE,
                        ownerId
                    )
                )
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
