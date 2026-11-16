package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.StaticAbility

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

    /**
     * The printed static abilities [entityId] has right now: none while it is face down (a
     * face-down permanent has no abilities) or while an effect says it loses all abilities
     * (Dance of the Skywise, Turn to Frog). Every rule that reads an attacker's or blocker's own
     * printed restriction goes through here, so Silumgar Assassin's "creatures with power greater
     * than this creature's power can't block it" is off while it is a face-down 2/2.
     */
    fun printedStatics(entityId: EntityId): List<StaticAbility> {
        if (projected.hasLostAllAbilities(entityId)) return emptyList()
        return state.getEntity(entityId)?.faceUpCardDefinition(cardRegistry)?.staticAbilities.orEmpty()
    }
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
