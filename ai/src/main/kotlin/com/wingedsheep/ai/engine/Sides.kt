package com.wingedsheep.ai.engine

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Who the AI is playing against — and, in a team format, who it is playing *with*.
 *
 * This replaces the 1v1 `soleOpponent` shortcut that every evaluation feature used to open with.
 * That helper was `getOpponents(playerId).firstOrNull()`, so outside a two-player game the AI
 * evaluated against **one arbitrary opponent** — blind to every other one, blind to its own
 * teammate's board, and (in Two-Headed Giant) reading a per-player `LifeTotalComponent` that the
 * engine stops maintaining once life is pooled on the team's canonical owner.
 *
 * In a two-player game there is exactly one opposing side of exactly one player, so every helper
 * here collapses to the old expression and produces bit-identical numbers. That is load-bearing,
 * not incidental: `FrozenBaselineTest` pins V0's 1v1 action stream to a hash, and this change is
 * not allowed to move it.
 *
 * @property mine the AI's own side — itself plus any still-in teammates, in turn order.
 * @property opponents one entry per opposing team that still has a player in the game, in turn
 *   order. Never empty; [sidesFor] returns null instead.
 */
internal class Sides(
    val mine: List<EntityId>,
    val opponents: List<List<EntityId>>,
) {
    /**
     * Fold a per-opponent score into the single number a feature returns.
     *
     * [scoreAgainst] is the AI's score facing one opposing side, higher = better for the AI. For a
     * plain differential that is `myValue - theirValue`; for `ThreatAssessment` it is the whole
     * race calculation against that side.
     */
    fun against(
        aggregate: OpponentAggregate,
        scoreAgainst: (opponent: List<EntityId>) -> Double,
    ): Double {
        // Short-circuited for correctness, not just for speed: THREAT blends a min with a mean,
        // and `w * x + (1 - w) * x` is not guaranteed to reproduce `x` exactly in floating point.
        // A 1v1 evaluation that differs from V0 in the last bit is a frozen-baseline failure.
        if (opponents.size == 1) return scoreAgainst(opponents[0])
        return aggregate.fold(opponents.map(scoreAgainst))
    }
}

/**
 * How a per-opponent score folds into one number when there is more than one opposing side.
 *
 * Both modes are invisible in 1v1 and in Two-Headed Giant (one opposing side either way) — they
 * only separate in a free-for-all pod.
 */
internal enum class OpponentAggregate {

    /**
     * Threat-shaped: the opponent the AI is doing worst against dominates, with a minority weight
     * on the field.
     *
     * Pure `min` — "me vs. the strongest opponent" — is the obvious reading, and it is wrong in two
     * ways that matter to a greedy evaluator. It has no gradient against anyone but the leader, so
     * killing a non-leader's creature scores exactly zero; and it is discontinuous, so two
     * near-equal opponents make the score flip between them on noise. The blend keeps the leader
     * dominant while leaving a real slope everywhere else.
     */
    THREAT {
        override fun fold(scores: List<Double>): Double =
            LEADER_WEIGHT * scores.min() + (1.0 - LEADER_WEIGHT) * scores.average()
    },

    /**
     * Positional: the AI's standing relative to the field, as a plain mean. For resources that do
     * not threaten anyone by themselves — life, cards in hand, mana development.
     */
    FIELD {
        override fun fold(scores: List<Double>): Double = scores.average()
    };

    internal abstract fun fold(scores: List<Double>): Double

    companion object {
        /**
         * How much of [THREAT] is the worst matchup rather than the field average. Hand-picked like
         * every other constant in `BoardFeatures.kt`; Phase 9's logistic fit is where it stops
         * being a guess.
         */
        const val LEADER_WEIGHT = 0.75
    }
}

/**
 * The [Sides] of the game from [playerId]'s point of view, or null when no opponent is left — the
 * game is already decided, and every feature returns 0.0 rather than inventing a differential
 * against nobody. (That was also the old `?: return 0.0` behaviour, reached for the same reason.)
 */
internal fun GameState.sidesFor(playerId: EntityId): Sides? {
    val opponents = getOpponents(playerId)
    if (opponents.isEmpty()) return null
    val mine = teamActivePlayers(playerId).ifEmpty { listOf(playerId) }

    // Fast path for every two-player game: getOpponents already excluded the whole of my team, and
    // without TeamComponents each opponent is its own side.
    if (mine.size == 1 && opponents.size == 1) return Sides(mine, listOf(opponents))

    val byTeam = LinkedHashMap<EntityId, MutableList<EntityId>>()
    for (opponent in opponents) {
        // teamOf(x).first() is a stable canonical key — the same one the engine uses for pooled
        // life (GameState.teamLifeOwnerOf) — and is just `x` in a game without teams.
        byTeam.getOrPut(teamOf(opponent).first()) { mutableListOf() }.add(opponent)
    }
    return Sides(mine, byTeam.values.toList())
}

/**
 * The distinct life totals on [side] (CR 810.4): one entry per *pool*, so a Two-Headed Giant team
 * contributes its single shared total once rather than counting the same 30 twice. In every
 * non-pooled format this is one entry per player.
 */
internal fun GameState.lifePoolsOf(side: List<EntityId>): List<Int> =
    if (side.size == 1) listOf(lifeTotal(side[0]))
    else side.map { teamLifeOwnerOf(it) }.distinct().map { lifeTotal(it) }

/**
 * Whether [candidate] is an opponent of [playerId] — a player on an opposing team (CR 810), not
 * merely "not me". In a game without teams this is exactly `candidate != playerId`.
 *
 * Returns false for anything that is not a player, so it is safe to call on a mixed list of
 * players and permanents (`validAttackTargets` carries planeswalkers, `DistributeDecision.targets`
 * carries creatures).
 */
internal fun GameState.isOpponentTo(candidate: EntityId, playerId: EntityId): Boolean =
    candidate != playerId && candidate in turnOrder && candidate !in teamOf(playerId)

/**
 * *An* opponent of [playerId], arbitrarily the first in turn order.
 *
 * Only honest where the answer genuinely does not depend on which opponent it is — coarse
 * per-card advisor heuristics such as "is giving away a card expensive right now". Anything that
 * evaluates a position must use [sidesFor] and fold over every opposing side; picking one and
 * ignoring the rest is the bug Phase 3 of `backlog/engine-ai-improvement.md` removed.
 */
internal fun GameState.anyOpponent(playerId: EntityId): EntityId? =
    getOpponents(playerId).firstOrNull()
