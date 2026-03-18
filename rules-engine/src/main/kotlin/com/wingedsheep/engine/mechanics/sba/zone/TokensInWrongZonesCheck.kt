package com.wingedsheep.engine.mechanics.sba.zone

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * 704.5s - Tokens in zones other than battlefield cease to exist.
 */
class TokensInWrongZonesCheck : StateBasedActionCheck {
    override val name = "704.5s Tokens in Wrong Zones"
    override val order = SbaOrder.TOKENS_IN_WRONG_ZONES

    override fun check(state: GameState): ExecutionResult {
        var newState = state

        val tokensToRemove = mutableListOf<Pair<EntityId, ZoneKey>>()

        for (playerId in state.turnOrder) {
            for (zoneType in listOf(Zone.HAND, Zone.GRAVEYARD, Zone.LIBRARY, Zone.EXILE)) {
                val zoneKey = ZoneKey(playerId, zoneType)
                for (entityId in state.getZone(zoneKey)) {
                    val container = state.getEntity(entityId) ?: continue
                    if (container.has<TokenComponent>()) {
                        tokensToRemove.add(entityId to zoneKey)
                    }
                }
            }
        }

        // Also check stack for token spells
        for (entityId in state.stack) {
            val container = state.getEntity(entityId) ?: continue
            if (container.has<TokenComponent>()) {
                newState = newState.removeFromStack(entityId)
                newState = newState.withoutEntity(entityId)
            }
        }

        for ((entityId, zoneKey) in tokensToRemove) {
            newState = newState.removeFromZone(zoneKey, entityId)
            newState = newState.withoutEntity(entityId)
        }

        return ExecutionResult.success(newState)
    }
}
