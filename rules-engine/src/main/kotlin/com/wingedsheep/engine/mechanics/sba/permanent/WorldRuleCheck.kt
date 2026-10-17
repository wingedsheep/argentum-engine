package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * 704.5k - World rule: if two or more permanents have the supertype world, all except the one
 * that has had the world supertype for the shortest amount of time are put into their owners'
 * graveyards; on a tie for the shortest time, all of them are.
 *
 * Unlike the legend rule this spans every player's battlefield and asks nobody to choose. The
 * world supertype is read from the projected state, and "for the shortest amount of time" is
 * measured from battlefield entry ([BattlefieldEntryTimestampComponent]): no implemented effect
 * grants the world supertype to a permanent already on the battlefield (a copy effect such as
 * Mirrorweave would — revisit this when one lands). Every entry ticks the global timestamp, so
 * permanents that entered in one simultaneous event (Open the Vaults returning two world
 * enchantments) carry different stamps; their shared
 * [BattlefieldEntryTimestampComponent.simultaneousEntryGroup] is what makes them a tie.
 */
class WorldRuleCheck(private val zones: ZoneTransitionService) : StateBasedActionCheck {
    override val name = "704.5k World Rule"
    override val order = SbaOrder.WORLD_RULE

    override fun check(state: GameState): ExecutionResult {
        val projected = state.projectedState
        val worlds = state.getBattlefield().filter { projected.isWorld(it) }
        if (worlds.size < 2) return ExecutionResult.success(state)

        val entries = worlds.associateWith { state.getEntity(it)?.get<BattlefieldEntryTimestampComponent>() }
        fun stamp(entityId: EntityId): Long = entries[entityId]?.timestamp ?: Long.MIN_VALUE
        val newest = worlds.maxBy { stamp(it) }
        val newestGroup = entries[newest]?.simultaneousEntryGroup
        val tied = worlds.any { other ->
            other != newest && (stamp(other) == stamp(newest) ||
                (newestGroup != null && entries[other]?.simultaneousEntryGroup == newestGroup))
        }
        val survivor = if (tied) null else newest

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (entityId in worlds) {
            if (entityId == survivor) continue
            val cardComponent = newState.getEntity(entityId)?.get<CardComponent>() ?: continue
            val result = SbaZoneMovementHelper.putPermanentInGraveyard(zones, newState, entityId, cardComponent)
            newState = result.newState
            events.addAll(result.events)
        }
        return ExecutionResult.success(newState, events)
    }
}
