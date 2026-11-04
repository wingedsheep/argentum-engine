package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PriorityChangedEvent
import com.wingedsheep.engine.core.TeamBlockerChoiceEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.TeamBlockerChoicesComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration

/**
 * Normally teammates submit one combined block. When effects give different players control of
 * their choices, collect those choices separately before validating or declaring any blockers.
 */
object SharedTeamBlockChoices {
    fun requiresSeparateChoices(state: GameState, playerId: EntityId): Boolean {
        val team = state.sharedTurnTeam(playerId)
        if (team.size < 2) return false
        val controllers = team.map { member ->
            CombatDeclarationControl.controllerOf(state, CombatDeclaration.BLOCKERS, member)
        }
        val actors = team.mapIndexed { index, member -> controllers[index] ?: state.actorFor(member) }
        val forced = controllers.any { it != null } || team.any { state.actorFor(it) !in team }
        return forced && actors.distinct().size > 1
    }

    private fun holder(state: GameState, playerId: EntityId): EntityId =
        state.turnOrder.first { it in state.sharedTurnTeam(playerId) }

    private fun choices(state: GameState, playerId: EntityId): TeamBlockerChoicesComponent =
        state.getEntity(holder(state, playerId))?.get<TeamBlockerChoicesComponent>()
            ?: TeamBlockerChoicesComponent()

    fun proposals(state: GameState, playerId: EntityId): Map<EntityId, List<EntityId>> =
        choices(state, playerId).choices.values.flatMap { it.entries }.associate { it.toPair() }

    fun problem(state: GameState, playerId: EntityId): String? = choices(state, playerId).error

    fun validateInput(state: GameState, playerId: EntityId, blockers: Map<EntityId, List<EntityId>>): String? {
        if (!requiresSeparateChoices(state, playerId)) return null
        if (playerId != state.priorityPlayerId) return "Wait for your team's current blocker choice"
        if (blockers.keys.any { state.projectedState.getController(it) != playerId }) {
            return "Choose only blockers controlled by this player"
        }
        return null
    }

    /** Null means this declaration can be submitted directly as a combined team choice. */
    fun submit(
        state: GameState,
        playerId: EntityId,
        blockers: Map<EntityId, List<EntityId>>,
        declare: (GameState, Map<EntityId, List<EntityId>>) -> ExecutionResult,
    ): ExecutionResult? {
        if (!requiresSeparateChoices(state, playerId)) return null
        validateInput(state, playerId, blockers)?.let { return ExecutionResult.error(state, it) }
        val holder = holder(state, playerId)
        val choices = choices(state, playerId).choices + (playerId to blockers)
        val team = state.turnOrder.filter { it in state.sharedTurnTeam(playerId) }
        val next = team.firstOrNull { it !in choices }
        if (next != null) {
            val staged = state.updateEntity(holder) {
                it.with(TeamBlockerChoicesComponent(choices))
            }.withPriority(next)
            return ExecutionResult.success(staged, listOf(
                TeamBlockerChoiceEvent(playerId, "Blocker choices saved; waiting for the rest of the team"),
                PriorityChangedEvent(next),
            ))
        }
        val combined = choices.values.flatMap { it.entries }.associate { it.toPair() }
        val cleared = state.updateEntity(holder) { it.without<TeamBlockerChoicesComponent>() }
        val result = declare(cleared, combined)
        val error = result.error ?: return result
        // Returning an error would restore the last submitted proposal at the transaction boundary.
        // Commit an explicit retry instead: none of the illegal combined declaration takes effect.
        val restart = state.updateEntity(holder) {
            it.with(TeamBlockerChoicesComponent(error = error))
        }.withPriority(team.first())
        return ExecutionResult.success(restart, listOf(
            TeamBlockerChoiceEvent(playerId, "Choose blockers again: $error"),
            PriorityChangedEvent(team.first()),
        ))
    }
}
