package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.CombatAdvisor
import com.wingedsheep.ai.engine.evaluation.EvaluationWeights
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The playout engine against real positions.
 *
 * The unit tests next door prove the *allocation* is sound; this proves a playout is a real game.
 * The properties worth pinning are the ones a broken playout would still produce a plausible number
 * for: it must stay inside its horizon, it must be reproducible from its seed, it must *vary* when
 * the seed varies (a deterministic policy collapses R samples into one), and it must read a
 * lopsided position as lopsided.
 */
class PlayoutEngineTest : ScenarioTestBase() {

    private fun engineFor(settings: RolloutSettings = RolloutSettings.DEFAULT): PlayoutEngine {
        val evaluator = EvaluationWeights.DEFAULT.toEvaluator()
        return PlayoutEngine(
            cardRegistry = cardRegistry,
            evaluator = evaluator,
            policy = PlayoutPolicy(
                CombatAdvisor(GameSimulator(cardRegistry), evaluator, cardRegistry),
                settings = settings,
            ),
            decisions = FastDecisionResponder(),
            settings = settings,
        )
    }

    /**
     * An even creature board with the AI on the play, holding priority in its own main phase.
     *
     * The libraries are load-bearing. `ScenarioBuilder.withPlayers` leaves them empty, which a
     * 1-ply agent never notices — a single simulated action does not cross a draw step — but a
     * playout runs two turns forward and would deck both players, so every line would end in a
     * decking race decided by whose draw step comes first (CR 104.3c). Stocking them is what makes
     * these positions measure Magic rather than the harness.
     */
    private fun evenPosition(): Pair<GameState, EntityId> {
        val game = scenario()
            .withPlayers()
            .withRngSeed(20260728L)
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(1, "Forest")
            .withLandsOnBattlefield(2, "Mountain", 3)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLibraries()
            .build()
        return game.state to game.player1Id
    }

    /** Stock both libraries deep enough that no horizon here reaches the bottom. */
    private fun ScenarioTestBase.ScenarioBuilder.withLibraries(): ScenarioTestBase.ScenarioBuilder =
        also { builder -> repeat(20) { builder.withCardInLibrary(1, "Forest"); builder.withCardInLibrary(2, "Mountain") } }

    init {
        test("a playout stops at its horizon rather than running to the end of the game") {
            val (state, playerId) = evenPosition()
            val value = engineFor().run(state, playerId, seed = 7L, horizonPlayerTurns = 2, baseline = 0.0)
            // A live board evaluates strictly between the terminal extremes. Hitting 0 or 1 here
            // would mean the playout ran past its horizon to a decided game.
            value shouldBeGreaterThan WinProbability.LOSS
            value shouldBeLessThan WinProbability.WIN
        }

        test("the same seed replays the same playout") {
            val (state, playerId) = evenPosition()
            val engine = engineFor()
            val first = engine.run(state, playerId, seed = 42L, horizonPlayerTurns = 2, baseline = 0.0)
            engine.run(state, playerId, seed = 42L, horizonPlayerTurns = 2, baseline = 0.0) shouldBe first
        }

        test("different seeds explore different lines — the policy is genuinely stochastic") {
            val (state, playerId) = evenPosition()
            val engine = engineFor()
            // A deterministic policy returns one value for every seed, which collapses R rollouts
            // into a single sample and makes the whole mechanism a slower static evaluator.
            val values = (1L..32L).map { engine.run(state, playerId, it, horizonPlayerTurns = 3, baseline = 0.0) }
            values.distinct().size shouldBeGreaterThan 1
        }

        test("every value a playout returns is a probability") {
            val (state, playerId) = evenPosition()
            val engine = engineFor()
            (1L..16L).forEach { seed ->
                val value = engine.run(state, playerId, seed, horizonPlayerTurns = 2, baseline = 0.0)
                value shouldBeGreaterThanOrEqual WinProbability.LOSS
                value shouldBeLessThanOrEqual WinProbability.WIN
            }
        }

        test("a hopeless position plays out worse than an even one") {
            val (even, evenPlayer) = evenPosition()
            val losing = scenario()
                .withPlayers()
                .withRngSeed(20260728L)
                .withLifeTotal(1, 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLifeTotal(2, 20)
                .withLandsOnBattlefield(2, "Mountain", 6)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLibraries()
                .build()

            val engine = engineFor()
            val evenMean = (1L..12L).map { engine.run(even, evenPlayer, it, 2, baseline = 0.0) }.average()
            val losingMean = (1L..12L)
                .map { engine.run(losing.state, losing.player1Id, it, 2, baseline = 0.0) }.average()
            losingMean shouldBeLessThan evenMean
        }

        test("the rollout agent proposes a legal move on a real position") {
            val (state, playerId) = evenPosition()
            val action = AIPlayer.create(cardRegistry, playerId, AiProfile.PHASE7).chooseAction(state)
            // Phase 1 measured the AI proposing ~0.9 illegal actions per game, and a new evaluator
            // is exactly the kind of change that reintroduces that. Legality is part of the bar.
            ActionProcessor(cardRegistry).process(state, action).result.error shouldBe null
        }

        test("the rollout agent is deterministic — the same position gives the same move") {
            val (state, playerId) = evenPosition()
            val first = AIPlayer.create(cardRegistry, playerId, AiProfile.PHASE7).chooseAction(state)
            val second = AIPlayer.create(cardRegistry, playerId, AiProfile.PHASE7).chooseAction(state)
            // `ArenaHarnessTest` asserts identical outcomes at 8 threads and at 1. A rollout search
            // that read a clock or a shared counter would break that silently, one game in ten.
            second shouldBe first
        }
    }
}
