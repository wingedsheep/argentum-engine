package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.sdk.model.EntityId

/** Shared bookkeeping for declared blocks and blocks created by resolving effects. */
internal object BlockingRelationships {
    fun establish(state: GameState, pairs: Map<EntityId, List<EntityId>>): GameState {
        var result = state
        val projected = state.projectedState
        for ((blocker, attackers) in pairs) {
            val existing = result.getEntity(blocker)?.get<BlockingComponent>()?.blockedAttackerIds.orEmpty()
            result = result.updateEntity(blocker) { c ->
                val partners = c.get<CombatPartnersThisTurnComponent>() ?: CombatPartnersThisTurnComponent()
                c.with(BlockingComponent((existing + attackers).distinct()))
                    .with(BlockedThisCombatComponent).with(BlockedThisTurnComponent)
                    .with(partners.copy(blockedIds = partners.blockedIds + attackers))
            }
            val blockerRef = state.objectRef(blocker)
            for (attacker in attackers) {
                result = result.updateEntity(attacker) { c ->
                    val blockers = c.get<BlockedComponent>()?.blockerIds.orEmpty()
                    val history = c.get<BlockersThisCombatComponent>()?.blockers.orEmpty()
                    val partners = c.get<CombatPartnersThisTurnComponent>() ?: CombatPartnersThisTurnComponent()
                    val controller = projected.getController(attacker)
                    var updated = c.with(BlockedComponent((blockers + blocker).distinct()))
                        .with(
                            partners.copy(
                                blockedByIds = partners.blockedByIds + blocker,
                                controllerWhenBlockedBy = if (controller == null) partners.controllerWhenBlockedBy
                                else partners.controllerWhenBlockedBy + (blocker to controller)
                            )
                        )
                    if (blockerRef != null) updated = updated.with(BlockersThisCombatComponent(history + blockerRef))
                    if (projected.isLegendary(blocker)) updated = updated.with(BlockedOrWasBlockedByLegendaryThisTurnComponent)
                    updated
                }
                if (projected.isLegendary(attacker)) result = result.updateEntity(blocker) {
                    it.with(BlockedOrWasBlockedByLegendaryThisTurnComponent)
                }
            }
        }
        return result
    }
}
