package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent
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
    ): EffectResult {
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
                val oldObjectRef = newState.objectRef(cardId)
                newState = newState.addToZone(ownerExileZone, cardId)
                // Same stamp ZoneTransitionService writes on an effect-driven exile, so a later
                // CR 610.3 "return it to its previous zone" sends these back to the graveyard
                // rather than taking CardDestination.ToZoneExiledFrom's battlefield fallback.
                newState = newState.updateEntity(cardId) { c ->
                    c.with(ExiledFromZoneComponent(Zone.GRAVEYARD))
                }
                events.add(
                    ZoneChangeEvent(
                        cardId,
                        cardComp?.name ?: "Unknown",
                        Zone.GRAVEYARD,
                        Zone.EXILE,
                        ownerId,
                        oldObject = oldObjectRef,
                        newObject = newState.objectRef(cardId)
                    )
                )
            }
        }

        return EffectResult.success(newState, events)
    }
}
