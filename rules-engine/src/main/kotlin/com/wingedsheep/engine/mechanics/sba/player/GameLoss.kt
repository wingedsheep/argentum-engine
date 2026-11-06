package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.replacement.ActiveReplacements
import com.wingedsheep.engine.replacement.PendingReplacementRider
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ReplaceGameLossWith
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The single path by which a player loses the game (CR 104.3) — every state-based loss check and
 * the "loses the game" effect go through [lose], so a "can't lose the game" grant and a
 * [ReplaceGameLossWith] replacement are honoured identically however the loss comes about.
 */
internal object GameLoss {

    /**
     * Make [playerId] lose the game for [reason], unless it already lost, can't lose
     * ([playerCantLoseGame]), or the loss is replaced. A replaced loss queues the replacement's rest
     * as a [PendingReplacementRider] (run before the next state-based-action pass or instruction)
     * and the player stays in the game. While that rider is pending, any further loss of the same
     * player — or, where players lose as a team (CR 810.8a), of a teammate — is part of the same
     * replaced event and is absorbed.
     */
    fun lose(
        state: GameState,
        playerId: EntityId,
        reason: LossReason,
        endReason: GameEndReason,
        predicateEvaluator: PredicateEvaluator,
        message: String? = null
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(playerId) ?: return state to emptyList()
        if (container.has<PlayerLostComponent>()) return state to emptyList()
        if (playerCantLoseGame(state, playerId, predicateEvaluator = predicateEvaluator)) return state to emptyList()

        val team = (if (state.format.playersWinLoseAsTeam) state.teamOf(playerId) else listOf(playerId)).toHashSet()
        if (state.pendingReplacementRiders.any { it.replacesGameLoss && it.subjectId in team }) {
            return state to emptyList()
        }
        replacementFor(state, playerId, team)?.let { rider ->
            return state.copy(pendingReplacementRiders = state.pendingReplacementRiders + rider) to emptyList()
        }

        val lost = state.updateEntity(playerId) { it.with(PlayerLostComponent(reason)) }
        return lost to listOf(PlayerLostEvent(playerId, endReason, message))
    }

    /**
     * The first [ReplaceGameLossWith] in force whose "whose loss" pattern covers [playerId]. Two
     * applicable replacements are the affected player's choice (CR 616.1). Every printed one today
     * is Exquisite Archangel, so two copies under one controller are interchangeable and the
     * earliest is taken; the one case where the pick matters — each head of a Two-Headed Giant
     * team controlling one — still takes the earliest rather than asking.
     */
    private fun replacementFor(state: GameState, playerId: EntityId, team: Set<EntityId>): PendingReplacementRider? {
        for (active in ActiveReplacements.all(state) { it is ReplaceGameLossWith }) {
            // ActiveReplacements already drops a source that lost its abilities or is face down.
            val effect = active.effect as ReplaceGameLossWith
            val matches = when (effect.appliesTo.player) {
                Player.Each, Player.Any -> true
                Player.You -> active.controllerId in team
                Player.EachOpponent -> state.isOpponentOf(playerId, active.controllerId)
                else -> false
            }
            if (!matches) continue
            return PendingReplacementRider(
                effect = effect.replacementEffect,
                hostId = active.sourceId,
                controllerId = active.controllerId,
                subjectId = playerId,
                replacesGameLoss = true
            )
        }
        return null
    }
}
