package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Context passed to block evasion rules for evaluating whether a blocker can block an attacker.
 */
data class BlockCheckContext(
    val state: GameState,
    val projected: ProjectedState,
    val attackerId: EntityId,
    val blockerId: EntityId,
    /** Controller of the blocking creature, even when a teammate submits the declaration. */
    val blockingPlayer: EntityId,
    val cardRegistry: CardRegistry
) {
    fun publicName(entityId: EntityId): String = com.wingedsheep.engine.state.nameVisibleToAll(
        state, entityId, state.getEntity(entityId)
            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ?: "Creature")

    /** Attacker-relative defender; shared-team blocking does not change who it attacks. */
    val defendingPlayer: EntityId = state.getEntity(attackerId)
        ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
        ?.let { com.wingedsheep.engine.mechanics.combat.CombatDefenders.defendingPlayerOf(state, it, projected) }
        ?: blockingPlayer
}

/**
 * A combat rule that determines whether a specific blocker can block a specific attacker.
 *
 * Each implementation encapsulates one evasion ability or blocking restriction.
 * Returns an error message if the block is illegal, null if this rule doesn't prevent it.
 *
 * MTG Golden Rule: Restrictions ("can't") beat requirements ("must").
 * The engine iterates all rules; if ANY returns non-null, the block is illegal.
 */
interface BlockEvasionRule {
    fun check(ctx: BlockCheckContext): String?
}
