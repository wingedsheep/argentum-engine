package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.RedistributeLifeTotalsContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * The step logic behind [com.wingedsheep.sdk.scripting.effects.RedistributeLifeTotalsEffect],
 * shared by its executor (first step) and its resumer (every later step).
 *
 * The controller builds a permutation of the players' current life totals one player at a time:
 * "which total does this player get?". Only totals that the player may legally receive
 * (CR 119.7–8: no higher total for a player who can't gain life, no lower one for a player who
 * can't lose life) *and* that still leave every later player a legal total are offered, so the
 * walk can never paint itself into a corner. A player with a single legal total is assigned it
 * without a prompt — which is always true of the last player. Handing a player their own total
 * leaves them out of the redistribution, which is how "any number of players" is chosen.
 *
 * Once every player has a total, each gains or loses the difference from the pre-effect snapshot
 * through [DamageUtils.gainLife] / [DamageUtils.loseLife], so replacements and triggers see
 * ordinary life gain and loss.
 */
object LifeRedistribution {

    sealed interface Step {
        data class Ask(
            val question: (String) -> ChooseOptionDecision,
            val continuation: RedistributeLifeTotalsContinuation,
        ) : Step

        data class Done(val state: GameState, val events: List<GameEvent>) : Step
    }

    /** Snapshot the players and their totals, then walk to the first real question (or finish). */
    fun start(state: GameState, context: EffectContext, predicateEvaluator: PredicateEvaluator): Step {
        // One entry per life total: in a shared-life team game (2HG) the team's single total is the
        // unit, which is exactly "no more than one member of each team" (CR 810.9f).
        val rotated = state.activePlayers.let { active ->
            val i = active.indexOf(context.controllerId)
            if (i < 0) active else active.drop(i) + active.take(i)
        }
        val players = rotated
            .filter { state.getEntity(it)?.get<LifeTotalComponent>() != null }
            .map { state.teamLifeOwnerOf(it) }
            .distinct()
        val totals = players.map { state.lifeTotal(it) }
        if (players.size < 2 || totals.distinct().size < 2) return Step.Done(state, emptyList())

        val progress = RedistributeLifeTotalsContinuation(
            chooserId = context.controllerId,
            players = players,
            originalTotals = totals,
            canGain = players.map { !DamageUtils.isLifeGainPrevented(state, it) },
            canLose = players.map {
                DamageUtils.applyStaticLifeLossModification(state, it, 1, predicateEvaluator) > 0
            },
            assigned = emptyList(),
            optionValues = emptyList(),
            effectContext = context,
        )
        return advance(state, progress, predicateEvaluator)
    }

    /** Record [value] for the player the pending question was about, then continue. */
    fun answer(
        state: GameState,
        progress: RedistributeLifeTotalsContinuation,
        value: Int,
        predicateEvaluator: PredicateEvaluator,
    ): Step = advance(state, progress.copy(assigned = progress.assigned + value), predicateEvaluator)

    private fun advance(
        state: GameState,
        start: RedistributeLifeTotalsContinuation,
        predicateEvaluator: PredicateEvaluator,
    ): Step {
        var progress = start
        while (progress.assigned.size < progress.players.size) {
            val index = progress.assigned.size
            val choices = legalChoices(progress, index)
            // Unreachable while the snapshot holds (every player may keep their own total), but a
            // corner means the permutation can't be completed — then no part of it happens.
            if (choices.isEmpty()) return Step.Done(state, emptyList())
            if (choices.size == 1) {
                progress = progress.copy(assigned = progress.assigned + choices.single())
                continue
            }
            return Step.Ask(question(state, progress, index, choices), progress.copy(optionValues = choices))
        }
        return apply(state, progress, predicateEvaluator)
    }

    /** Distinct totals [index] may receive that still leave the later players a legal assignment. */
    private fun legalChoices(progress: RedistributeLifeTotalsContinuation, index: Int): List<Int> {
        val remaining = remainingTotals(progress)
        return remaining.distinct().sortedDescending().filter { value ->
            mayReceive(progress, index, value) &&
                completable(progress, index + 1, remaining.toMutableList().also { it.remove(value) })
        }
    }

    private fun remainingTotals(progress: RedistributeLifeTotalsContinuation): List<Int> =
        progress.originalTotals.toMutableList().also { pool -> progress.assigned.forEach { pool.remove(it) } }

    private fun mayReceive(progress: RedistributeLifeTotalsContinuation, index: Int, value: Int): Boolean {
        val current = progress.originalTotals[index]
        return when {
            value > current -> progress.canGain[index]
            value < current -> progress.canLose[index]
            else -> true
        }
    }

    /** Backtracking check that players `index..` can each take one of [pool] legally. */
    private fun completable(progress: RedistributeLifeTotalsContinuation, index: Int, pool: MutableList<Int>): Boolean {
        if (index == progress.players.size) return true
        for (value in pool.distinct()) {
            if (!mayReceive(progress, index, value)) continue
            pool.remove(value)
            val ok = completable(progress, index + 1, pool)
            pool.add(value)
            if (ok) return true
        }
        return false
    }

    private fun question(
        state: GameState,
        progress: RedistributeLifeTotalsContinuation,
        index: Int,
        choices: List<Int>,
    ): (String) -> ChooseOptionDecision {
        val playerId = progress.players[index]
        val own = progress.originalTotals[index]
        val sourceId = progress.effectContext.sourceId
        return { id ->
            ChooseOptionDecision(
                id = id,
                playerId = progress.chooserId,
                prompt = "Choose the life total ${playerName(state, playerId)} gets",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                    phase = DecisionPhase.RESOLUTION,
                ),
                options = choices.map { value -> if (value == own) "$value life (unchanged)" else "$value life" },
            )
        }
    }

    private fun playerName(state: GameState, playerId: EntityId): String =
        state.getEntity(playerId)?.get<PlayerComponent>()?.name ?: "Player"

    private fun apply(
        state: GameState,
        progress: RedistributeLifeTotalsContinuation,
        predicateEvaluator: PredicateEvaluator,
    ): Step {
        var newState = state
        val events = mutableListOf<GameEvent>()
        progress.players.forEachIndexed { i, playerId ->
            val from = progress.originalTotals[i]
            val to = progress.assigned[i]
            val (next, event) = when {
                to > from -> DamageUtils.gainLife(newState, playerId, to - from, predicateEvaluator = predicateEvaluator)
                to < from -> DamageUtils.loseLife(
                    newState, playerId, from - to,
                    reason = LifeChangeReason.LIFE_LOSS,
                    applyLifeLossModification = true,
                    predicateEvaluator = predicateEvaluator,
                )
                else -> newState to null
            }
            newState = next
            if (event != null) events.add(event)
        }
        return Step.Done(newState, events)
    }
}
