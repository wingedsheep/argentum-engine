package com.wingedsheep.engine.mechanics.sba.zone

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Rule 707.10a — a copy of a card in any zone other than the stack or the battlefield ceases
 * to exist. This is a state-based action.
 *
 * The "copy a card, then cast the copy" pattern (e.g. Shiko, Paragon of the Way) materialises a
 * copy in a non-stack zone before offering to cast it. When the copy is cast it moves onto the
 * stack (and either becomes a token on the battlefield or ceases to exist on resolution per
 * Rule 707.10) — so this check never touches a copy that is mid-cast, because state-based
 * actions are not run while resolution is paused. It only sweeps up copies that were declined
 * or could not be cast, leaving no phantom card lingering in the exile/graveyard/hand/library.
 *
 * Only stack-style copies are removed: those with [CopyOfComponent.originalCardComponent] left
 * null. Clone-style copies (Mockingbird, "becomes a copy of") snapshot the pre-copy component
 * and live on the battlefield as real permanents, so they are out of scope. Tokens are left to
 * the dedicated [TokensInWrongZonesCheck] (Rule 704.5s).
 */
class PhantomCardCopiesCheck : StateBasedActionCheck {
    override val name = "707.10a Phantom Card Copies"
    override val order = SbaOrder.PHANTOM_CARD_COPIES

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val copiesToRemove = mutableListOf<Pair<EntityId, ZoneKey>>()

        for (playerId in state.turnOrder) {
            for (zoneType in listOf(Zone.HAND, Zone.GRAVEYARD, Zone.LIBRARY, Zone.EXILE)) {
                val zoneKey = ZoneKey(playerId, zoneType)
                for (entityId in state.getZone(zoneKey)) {
                    val container = state.getEntity(entityId) ?: continue
                    if (container.has<TokenComponent>()) continue
                    val copyOf = container.get<CopyOfComponent>() ?: continue
                    if (copyOf.originalCardComponent == null) {
                        copiesToRemove.add(entityId to zoneKey)
                    }
                }
            }
        }

        for ((entityId, zoneKey) in copiesToRemove) {
            newState = newState.removeFromZone(zoneKey, entityId).withoutEntity(entityId)
        }

        return ExecutionResult.success(newState)
    }
}
