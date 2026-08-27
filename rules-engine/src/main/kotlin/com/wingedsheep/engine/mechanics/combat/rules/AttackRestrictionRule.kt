package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Context for attack restriction checks.
 */
data class AttackCheckContext(
    val state: GameState,
    val projected: ProjectedState,
    val attackerId: EntityId,
    val attackingPlayer: EntityId,
    val cardRegistry: CardRegistry
)

/**
 * Per-creature attack restriction: checks whether a creature is eligible to attack
 * regardless of which player/planeswalker it's attacking.
 *
 * Returns an error message if the creature CANNOT attack, null if this rule allows it.
 */
interface AttackRestrictionRule {
    fun check(ctx: AttackCheckContext): String?
}

/**
 * Per-defender attack restriction: checks whether a creature can attack a specific defender.
 * Used for mechanics like CantAttackUnless and CantBeAttackedBy where the defender matters.
 *
 * Returns an error message if the creature CANNOT attack this defender, null if allowed.
 */
interface AttackDefenderRule {
    fun check(ctx: AttackCheckContext, defenderId: EntityId): String?

    /**
     * Returns true if the creature is restricted from attacking ALL opponents and their planeswalkers.
     * Default implementation checks [check] against every opponent and their planeswalkers.
     * Used by [getValidAttackers] for must-attack requirement validation.
     */
    fun restrictsAllDefenders(ctx: AttackCheckContext): Boolean {
        // Real opponents only: not a teammate (CR 805.10b), not a seat that has left (CR 800.4a).
        val opponents = ctx.state.getOpponents(ctx.attackingPlayer)
        val projected = ctx.projected
        // Collect all valid defenders: opponent players + their planeswalkers
        val allDefenders = opponents.toMutableList()
        for (opponentId in opponents) {
            allDefenders.addAll(
                ctx.state.getBattlefield().filter { entityId ->
                    projected.isPlaneswalker(entityId) && projected.getController(entityId) == opponentId
                }
            )
        }
        return allDefenders.all { defenderId -> check(ctx, defenderId) != null }
    }
}
