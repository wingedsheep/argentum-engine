package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Shared "is this attacker blocked?" query behind
 * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsBlocked] and
 * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsUnblocked].
 *
 * CR 509.1h: once an attacking creature has been blocked it *remains* blocked for the rest of
 * combat, even if every creature blocking it is removed from combat or leaves the battlefield.
 * That durable status is what [BlockedComponent] encodes — the engine keeps it (with an emptied
 * blocker list) when blockers go away. Scanning live [BlockingComponent]s alone would wrongly
 * report such an attacker as unblocked, so the durable marker is checked first and the live scan
 * only backs it up for hand-built states that never went through the block phase.
 */
internal object CombatStatusQueries {

    fun isBlockedAttacker(state: GameState, entityId: EntityId, container: ComponentContainer): Boolean {
        if (!container.has<AttackingComponent>()) return false
        if (container.has<BlockedComponent>()) return true
        return state.getBattlefield().any { blockerId ->
            state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
        }
    }
}
