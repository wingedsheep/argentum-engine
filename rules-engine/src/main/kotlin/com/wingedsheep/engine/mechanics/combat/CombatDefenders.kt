package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Helpers for the *defending* side of multiplayer combat (CR 802.2).
 *
 * In a Free-for-All game the attacking player declares each creature against a specific
 * player or planeswalker, so a single combat can have several defending players at once.
 * "The defending player" is therefore not a single fixed seat — it is derived per attacking
 * creature from where that creature is attacking (CR 802.2a). These helpers answer "who is
 * defending in this combat" so block declaration can be offered to each of them, in turn
 * order starting from the active player (APNAP, CR 101.4).
 */
object CombatDefenders {

    /** The player defending against an attack aimed at [defenderId] (a player attacks as
     *  themselves; a planeswalker/battle defends on behalf of its controller). */
    fun defendingPlayerOf(state: GameState, defenderId: EntityId): EntityId =
        if (defenderId in state.turnOrder) {
            defenderId
        } else {
            state.getEntity(defenderId)?.get<ControllerComponent>()?.playerId ?: defenderId
        }

    /** Every distinct defending player in the current combat: anyone who has a creature attacking
     *  them (or their planeswalkers/battles) — and, under shared team turns (Two-Headed Giant), their
     *  whole team (CR 805.10a: every member of the nonactive team is a defending player, so an
     *  un-attacked teammate may still declare blockers to protect the team). Without shared team
     *  turns — Team vs. Team (CR 808) and non-team games — only the directly-attacked players defend
     *  (`sharedTurnTeam` is a singleton there), so a teammate can't block for you. */
    fun defendingPlayers(state: GameState): Set<EntityId> =
        state.getBattlefield()
            .mapNotNull { state.getEntity(it)?.get<AttackingComponent>()?.defenderId }
            .map { defendingPlayerOf(state, it) }
            .flatMap { state.sharedTurnTeam(it) }
            .toSet()

    /** True if [playerId] is a defending player in the current combat. */
    fun isDefendingPlayer(state: GameState, playerId: EntityId): Boolean =
        defendingPlayers(state).contains(playerId)

    /**
     * The opponents [attackingPlayer]'s creatures are allowed to attack under the game's
     * [com.wingedsheep.sdk.core.AttackMode] (CR 802 / 803). This is the single source of truth
     * for the attack-mode seat restriction: the legal-action enumerator filters its
     * `validAttackTargets` hint by it, and `AttackModeDefenderRule` enforces it at declaration.
     *
     * - [AttackMode.MULTIPLE] — every opponent still in the game (CR 802.2).
     * - [AttackMode.LEFT] — only the opponent in the next remaining seat (CR 803.1a). Turn order
     *   proceeds to the left (CR 103.7b), so "the player to your left" is [GameState.getNextPlayer].
     * - [AttackMode.RIGHT] — only the opponent in the previous remaining seat (CR 803.1b), via
     *   [GameState.getPreviousPlayer].
     *
     * A planeswalker/battle is attackable iff its controller/protector is in this set (the caller
     * maps it). Departed players are already skipped by the seat helpers, so in Free-for-All the
     * left/right neighbour is always an opponent exactly one seat away.
     */
    fun legalDefendingPlayers(state: GameState, attackingPlayer: EntityId): Set<EntityId> =
        when (state.attackMode) {
            com.wingedsheep.sdk.core.AttackMode.MULTIPLE -> state.getOpponents(attackingPlayer).toSet()
            com.wingedsheep.sdk.core.AttackMode.LEFT ->
                setOf(state.getNextPlayer(attackingPlayer)).minus(attackingPlayer)
            com.wingedsheep.sdk.core.AttackMode.RIGHT ->
                setOf(state.getPreviousPlayer(attackingPlayer)).minus(attackingPlayer)
        }

    /**
     * The defending players ordered for sequential block declaration: turn order starting
     * from the active player (CR 101.4 APNAP). The active player is never a defender, so in
     * practice this is the defenders in turn order after the active player, wrapping around.
     */
    fun defendingPlayersInApnapOrder(state: GameState): List<EntityId> {
        val defenders = defendingPlayers(state)
        if (defenders.isEmpty()) return emptyList()
        val order = state.turnOrder
        if (order.isEmpty()) return defenders.toList()
        val startIdx = state.activePlayerId?.let { order.indexOf(it) }?.coerceAtLeast(0) ?: 0
        return (order.indices)
            .map { order[(startIdx + it) % order.size] }
            .filter { it in defenders }
    }
}
