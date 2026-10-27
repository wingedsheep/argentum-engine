package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.player.CombatDeclarationControlComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import com.wingedsheep.sdk.model.EntityId

/**
 * Who makes a combat *declaration* (Master Warcraft: "You choose which creatures attack this turn.
 * You choose which creatures block this turn and how those creatures block."; Brutal Hordechief:
 * "you choose how those creatures block" for creatures your opponents control).
 *
 * A declaration is owed by the active player in an undeclared declare attackers step, and by each
 * defending player in an undeclared declare blockers step (CR 508.1 / 509.1). Normally the player
 * who owes it makes it; while a [CombatDeclarationControlComponent] grant covering that
 * declaration and that player is in force this turn, the player carrying it makes it instead. The
 * declaration itself is unchanged — it is still the owing player's `DeclareAttackers` /
 * `DeclareBlockers`, validated against their creatures and their requirements and restrictions —
 * only the *input* moves.
 *
 * Deliberately separate from [GameState.actorFor]: that one moves all input *and* the right to see
 * the controlled player's hidden zones (Mindslaver), while this moves exactly two choices and
 * nothing else.
 */
object CombatDeclarationControl {

    /**
     * The declaration [playerId] owes right now and hasn't made yet, or null when they owe none.
     */
    fun owedDeclaration(state: GameState, playerId: EntityId): CombatDeclaration? {
        val entity = state.getEntity(playerId) ?: return null
        if (state.step == Step.DECLARE_ATTACKERS && state.isActiveTurnFor(playerId)) {
            return CombatDeclaration.ATTACKERS.takeIf { entity.get<AttackersDeclaredThisCombatComponent>() == null }
        }
        if (state.step == Step.DECLARE_BLOCKERS && !state.isActiveTurnFor(playerId)) {
            return CombatDeclaration.BLOCKERS.takeIf {
                entity.get<BlockersDeclaredThisCombatComponent>() == null &&
                    CombatDefenders.isDefendingPlayer(state, playerId)
            }
        }
        return null
    }

    /**
     * The player who makes [declaration] for [playerId] this turn, or null when nobody took it
     * over: the holder of the latest grant this turn that covers both (Brutal Hordechief ruling —
     * the last such effect to resolve decides, for the players it covers).
     */
    fun controllerOf(state: GameState, declaration: CombatDeclaration, playerId: EntityId): EntityId? =
        state.turnOrder
            .filter { it in state.activePlayers }
            .mapNotNull { holder ->
                state.getEntity(holder)?.get<CombatDeclarationControlComponent>()?.grants
                    ?.filter {
                        it.turnNumber == state.turnNumber &&
                            declaration in it.declarations &&
                            playerId in it.declaringPlayers
                    }
                    ?.maxOfOrNull { it.timestamp }
                    ?.let { holder to it }
            }
            .maxByOrNull { it.second }
            ?.first

    /**
     * The player who takes over the declaration [playerId] owes right now, or null when [playerId]
     * (or whoever already acts for them) makes it as usual.
     */
    fun declarerFor(state: GameState, playerId: EntityId): EntityId? {
        val owed = owedDeclaration(state, playerId) ?: return null
        return controllerOf(state, owed, playerId)
    }

    /**
     * Whose input the game is waiting on for [playerId]'s current window: the combat-declaration
     * controller while [playerId] owes a declaration it has taken over, otherwise the usual
     * [GameState.actorFor]. Use for *routing input* (legal actions, authorization, auto-pass) —
     * never for visibility, which stays with [GameState.actorFor].
     */
    fun inputActorFor(state: GameState, playerId: EntityId): EntityId =
        declarerFor(state, playerId) ?: state.actorFor(playerId)
}
