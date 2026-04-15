package com.wingedsheep.engine.gym.trainer.selfplay

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.gym.GameEnvironment
import com.wingedsheep.engine.gym.trainer.search.AlphaZeroSearch
import com.wingedsheep.engine.gym.trainer.search.MctsEdge
import com.wingedsheep.engine.gym.trainer.spi.ActionFeaturizer
import com.wingedsheep.engine.gym.trainer.spi.Evaluator
import com.wingedsheep.engine.gym.trainer.spi.SelfPlaySink
import com.wingedsheep.engine.gym.trainer.spi.StateFeaturizer
import com.wingedsheep.engine.gym.trainer.spi.StructuredDecisionResolver
import com.wingedsheep.engine.gym.trainer.spi.TrainerContext
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID
import kotlin.math.pow
import kotlin.random.Random

/**
 * Drives self-play games end-to-end, feeding MCTS visit counts into a
 * [SelfPlaySink].
 *
 * Temperature schedule follows AlphaZero convention:
 *  - For the first [temperatureMoves] decisions, sample the next action
 *    from `visits^(1/temperature)`.
 *  - After that, take the argmax (highest-visit edge).
 *
 * Setting [temperature] to `0.0` is also supported and gives pure argmax
 * from the first move.
 *
 * The loop owns one [AlphaZeroSearch] per env. It is single-threaded; for
 * parallel self-play, run many `SelfPlayLoop`s in parallel — each with its
 * own env fork and its own search.
 */
class SelfPlayLoop<T>(
    private val envFactory: () -> GameEnvironment,
    private val featurizer: StateFeaturizer<T>,
    private val actionFeaturizer: ActionFeaturizer,
    private val evaluator: Evaluator<T>,
    private val sink: SelfPlaySink<T>,
    private val structuredResolver: StructuredDecisionResolver =
        com.wingedsheep.engine.gym.trainer.search.RandomStructuredResolver(),
    private val simulationsPerMove: Int = 100,
    private val cPuct: Double = 1.0,
    private val dirichletAlpha: Double? = 0.3,
    private val dirichletWeight: Double = 0.25,
    private val temperature: Double = 1.0,
    private val temperatureMoves: Int = 30,
    private val maxSteps: Int = 2000,
    private val rng: Random = Random.Default
) {
    /** Run one game. Returns the winner (null = draw / truncation). */
    fun playGame(config: GameConfig, gameId: String = UUID.randomUUID().toString()): GameOutcome {
        val env = envFactory()
        env.reset(config)

        sink.beginGame(gameId, env.playerIds)

        var stepCount = 0
        while (!env.isTerminal && stepCount < maxSteps) {
            val acting = env.agentToAct ?: break

            val search = AlphaZeroSearch(
                env = env,
                featurizer = featurizer,
                actionFeaturizer = actionFeaturizer,
                evaluator = evaluator,
                structuredResolver = structuredResolver,
                cPuct = cPuct,
                dirichletAlpha = dirichletAlpha,
                dirichletWeight = dirichletWeight,
                rng = rng
            )
            val result = search.run(simulationsPerMove)

            val edges = result.root.edges
            if (edges.isEmpty()) break

            val ctx = TrainerContext(env.state, acting, env.pendingDecision)
            val features = featurizer.featurize(ctx)
            val legalSlots = edges.map { it.slot }
            val visits = IntArray(edges.size) { edges[it].visits }
            val headUsed = legalSlots.firstOrNull()?.head ?: "unknown"

            sink.recordStep(
                features = features,
                ctx = ctx,
                actingPlayer = acting,
                headUsed = headUsed,
                legalSlots = legalSlots,
                visits = visits,
                mctsValue = result.rootValue
            )

            val chosen = if (stepCount < temperatureMoves && temperature > 0.0) {
                sampleByTemperature(edges, temperature)
            } else {
                edges.maxByOrNull { it.visits } ?: edges.first()
            }

            env.step(chosen.action)
            stepCount += 1
        }

        val winner = env.winnerId
        sink.endGame(winner)
        return GameOutcome(
            gameId = gameId,
            winner = winner,
            stepCount = stepCount,
            truncated = !env.isTerminal
        )
    }

    /** Play [count] games sequentially. */
    fun playGames(count: Int, configFactory: (Int) -> GameConfig): List<GameOutcome> =
        (0 until count).map { i -> playGame(configFactory(i)) }

    // =========================================================================
    // Internals
    // =========================================================================

    private fun sampleByTemperature(edges: List<MctsEdge>, temp: Double): MctsEdge {
        val weights = DoubleArray(edges.size)
        var sum = 0.0
        for ((i, e) in edges.withIndex()) {
            val w = if (e.visits == 0) 0.0 else (e.visits.toDouble()).pow(1.0 / temp)
            weights[i] = w
            sum += w
        }
        if (sum <= 0.0) return edges.first()
        val r = rng.nextDouble() * sum
        var acc = 0.0
        for ((i, w) in weights.withIndex()) {
            acc += w
            if (r <= acc) return edges[i]
        }
        return edges.last()
    }
}

/** Summary of one self-play game. */
data class GameOutcome(
    val gameId: String,
    val winner: EntityId?,
    val stepCount: Int,
    /** true when [SelfPlayLoop.maxSteps] hit without a terminal state. */
    val truncated: Boolean
)
