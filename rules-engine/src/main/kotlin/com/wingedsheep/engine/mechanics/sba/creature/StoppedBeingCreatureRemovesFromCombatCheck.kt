package com.wingedsheep.engine.mechanics.sba.creature

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * CR 506.4 — "A permanent is removed from combat if … it's an attacking or blocking creature that
 * … stops being a creature, or becomes a battle."
 *
 * A Theros god whose devotion drops mid-combat (Nylea, God of the Hunt), an animated land whose
 * animation ends, or an Impending permanent that regains a time counter stops being a creature;
 * from then on it is no longer an attacking, blocking, blocked or unblocked creature, and it
 * doesn't rejoin combat if it becomes a creature again later (the Theros gods' ruling) — the
 * removal strips the combat components, and nothing puts them back.
 *
 * Reads *projected* types, since only projection sees the Layer 4 effect that took the creature
 * type away. A blocker that is also a planeswalker being attacked keeps its attacked status
 * (CR 506.4d): [CombatRemovalHelper] only strips the attacking/blocking side.
 */
class StoppedBeingCreatureRemovesFromCombatCheck : StateBasedActionCheck {
    override val name = "506.4 Stopped-Being-a-Creature Combat Removal"
    override val order = SbaOrder.STOPPED_BEING_CREATURE_COMBAT

    override fun check(state: GameState): ExecutionResult {
        val projected = state.projectedState

        val toRemove = mutableListOf<EntityId>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            if (!container.has<AttackingComponent>() && !container.has<BlockingComponent>()) continue
            if (!projected.isCreature(entityId) || projected.isBattle(entityId)) toRemove += entityId
        }
        if (toRemove.isEmpty()) return ExecutionResult.success(state)

        var newState = state
        for (entityId in toRemove) {
            newState = CombatRemovalHelper.removeFromCombat(newState, entityId)
        }
        return ExecutionResult.success(newState)
    }
}
