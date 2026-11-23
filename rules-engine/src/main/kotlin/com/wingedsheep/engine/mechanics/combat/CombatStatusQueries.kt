package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.mechanics.battle.Battles
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.sdk.core.Step
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

    /** The candidate blocks [sourceId] (CR 509): [sourceId] is in its blocked-attacker set. */
    fun isBlockingSource(container: ComponentContainer, sourceId: EntityId): Boolean =
        container.get<BlockingComponent>()?.blockedAttackerIds?.contains(sourceId) == true

    /** [sourceId] blocks the candidate (CR 509): the candidate is in the source's blocked-attacker set. */
    fun isBlockedBySource(state: GameState, entityId: EntityId, sourceId: EntityId): Boolean =
        state.getEntity(sourceId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true

    /** Either direction of the live combat pairing with [sourceId] — "blocking or blocked by". */
    fun isCombatPairedWithSource(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        sourceId: EntityId,
    ): Boolean = isBlockingSource(container, sourceId) || isBlockedBySource(state, entityId, sourceId)

    fun isBlockedAttacker(state: GameState, entityId: EntityId, container: ComponentContainer): Boolean {
        if (!container.has<AttackingComponent>()) return false
        if (container.has<BlockedComponent>()) return true
        return state.getBattlefield().any { blockerId ->
            state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
        }
    }

    /** Unblocked status starts at block declaration, not at attack declaration (CR 509.1h). */
    fun isUnblockedAttacker(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        projectedController: (EntityId) -> EntityId?,
    ): Boolean {
        val attacking = container.get<AttackingComponent>() ?: return false
        if (isBlockedAttacker(state, entityId, container)) return false
        return when (state.step) {
            Step.DECLARE_BLOCKERS -> {
                val defender = attacking.defenderId
                val defendingPlayer = if (defender in state.turnOrder) defender else
                    Battles.protectorOf(state, defender) ?: projectedController(defender)
                if (defendingPlayer != null) {
                    state.sharedTurnTeam(defendingPlayer).all {
                        state.getEntity(it)?.has<BlockersDeclaredThisCombatComponent>() == true
                    }
                } else {
                    // The attacked permanent can leave after blockers were declared.
                    state.turnOrder.any {
                        state.getEntity(it)?.has<BlockersDeclaredThisCombatComponent>() == true
                    }
                }
            }
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> true
            else -> false
        }
    }
}
