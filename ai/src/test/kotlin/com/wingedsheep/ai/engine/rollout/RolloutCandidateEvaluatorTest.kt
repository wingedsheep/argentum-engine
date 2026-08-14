package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.SearchAllowances
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * How the rollout budget is *allocated*, asserted against a recording stub rather than inferred
 * from win rates.
 *
 * Three properties carry the phase, and none of them are visible in an arena number:
 *
 * - **Reproducibility.** The arena's whole value rests on a rerun at the same seed being the same
 *   game — `ArenaHarnessTest` asserts it by playing at 8 threads and at 1 — and a search that read
 *   a clock or a counter would quietly break that.
 * - **Common random numbers.** Every candidate must play out against the *same* sampled futures,
 *   or the comparison is unpaired and needs ~4× the playouts for the same discrimination.
 * - **The budget is respected and concentrated.** Sequential halving must not overspend, and must
 *   give the contenders more than an even split would.
 */
class RolloutCandidateEvaluatorTest : ScenarioTestBase() {

    /** Records every playout it is asked for, and returns a value fixed per candidate state. */
    private class RecordingPlayouts(private val valueOf: (GameState) -> Double) : Playouts {
        val calls = mutableListOf<Pair<GameState, Long>>()

        override fun run(
            start: GameState,
            playerId: EntityId,
            seed: Long,
            horizonPlayerTurns: Int,
            baseline: Double,
        ): Double {
            calls += start to seed
            return valueOf(start)
        }

        fun countFor(state: GameState): Int = calls.count { it.first === state }

        fun seedsFor(state: GameState): List<Long> = calls.filter { it.first === state }.map { it.second }
    }

    private fun budget(tier: BudgetTier, playouts: Int) = DecisionBudget(
        tier,
        SearchAllowances.LEGACY.copy(rolloutPlayouts = playouts),
        DecisionBudget.UNBOUNDED_MILLIS,
    )

    /** Distinct states to score. Content is irrelevant — the stub decides their values. */
    private fun candidateStates(count: Int): List<GameState> {
        val initializer = GameInitializer(cardRegistry)
        return (0 until count).map { index ->
            initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("A", Deck(cards = emptyList())),
                        PlayerConfig("B", Deck(cards = emptyList())),
                    ),
                    skipMulligans = true,
                    startingPlayerIndex = 0,
                    seed = 1_000L + index,
                )
            ).state
        }
    }

    init {
        test("scores are a pure function of the position — the same call twice agrees exactly") {
            val states = candidateStates(4)
            val playerId = states.first().turnOrder.first()
            val root = states.first()
            // The stub's value depends only on the seed it is handed, so any nondeterminism in the
            // result can only have come from the evaluator's own seeding or allocation. A stub that
            // returned a constant would pass this test even if the grid were built from a clock.
            val seedDriven = Playouts { _, _, seed, _, _ -> ((seed ushr 40) and 0xFF) / 255.0 }
            val first = RolloutCandidateEvaluator(seedDriven, zeroEvaluator())
                .scoreAll(root, states, playerId, budget(BudgetTier.NORMAL, 32))
            val second = RolloutCandidateEvaluator(seedDriven, zeroEvaluator())
                .scoreAll(root, states, playerId, budget(BudgetTier.NORMAL, 32))
            first shouldContainExactly second
        }

        test("every candidate plays out against the same seed grid — common random numbers") {
            val states = candidateStates(4)
            val playerId = states.first().turnOrder.first()
            // Flat values so nothing is eliminated and every candidate walks the whole grid.
            val stub = RecordingPlayouts { WinProbability.DRAW }
            RolloutCandidateEvaluator(stub, zeroEvaluator())
                .scoreAll(states.first(), states, playerId, budget(BudgetTier.NORMAL, 64))

            val reference = stub.seedsFor(states.first())
            reference.size shouldBeGreaterThan 1
            states.drop(1).forEach { state ->
                val seeds = stub.seedsFor(state)
                // Prefix, not equality: a candidate eliminated in an early round stops walking the
                // grid, but the cells it *did* visit must be the same cells.
                seeds shouldContainExactly reference.take(seeds.size)
            }
        }

        test("different decisions draw different grids") {
            val states = candidateStates(2)
            val playerId = states.first().turnOrder.first()
            val stubA = RecordingPlayouts { WinProbability.DRAW }
            val stubB = RecordingPlayouts { WinProbability.DRAW }
            RolloutCandidateEvaluator(stubA, zeroEvaluator())
                .scoreAll(states[0], states, playerId, budget(BudgetTier.NORMAL, 8))
            RolloutCandidateEvaluator(stubB, zeroEvaluator())
                .scoreAll(states[1], states, playerId, budget(BudgetTier.NORMAL, 8))
            stubA.calls.first().second shouldNotBe stubB.calls.first().second
        }

        test("sequential halving stays inside the budget and concentrates it on the leader") {
            val states = candidateStates(8)
            val playerId = states.first().turnOrder.first()
            val leader = states.last()
            val stub = RecordingPlayouts { if (it === leader) 0.9 else 0.1 }

            val scores = RolloutCandidateEvaluator(stub, zeroEvaluator())
                .scoreAll(states.first(), states, playerId, budget(BudgetTier.NORMAL, 64))

            stub.calls.size shouldBeLessThanOrEqual 64
            // Even splitting would give every candidate 8. The leader survives every round, so it
            // must beat that — this is the 2–4× on the contenders the phase is built on.
            stub.countFor(leader) shouldBeGreaterThan 8
            // And the winner still reads as the winner after conversion back to raw score space.
            scores[states.indexOf(leader)] shouldBeGreaterThan scores[0]
        }

        test("with halving off, the budget is spread evenly instead") {
            val states = candidateStates(8)
            val playerId = states.first().turnOrder.first()
            val leader = states.last()
            val stub = RecordingPlayouts { if (it === leader) 0.9 else 0.1 }

            RolloutCandidateEvaluator(
                stub, zeroEvaluator(), RolloutSettings.DEFAULT.copy(sequentialHalving = false)
            ).scoreAll(states.first(), states, playerId, budget(BudgetTier.NORMAL, 64))

            val counts = states.map { stub.countFor(it) }.distinct()
            counts.size shouldBe 1
        }

        test("every candidate is scored after the first round — the anytime contract") {
            val states = candidateStates(8)
            val playerId = states.first().turnOrder.first()
            val stub = RecordingPlayouts { 0.5 }
            // One playout per candidate is all this affords, which is exactly the degenerate case
            // the contract exists for: a move must still come out.
            val scores = RolloutCandidateEvaluator(stub, zeroEvaluator())
                .scoreAll(states.first(), states, playerId, budget(BudgetTier.NORMAL, 8))
            scores.size shouldBe states.size
            states.forEach { stub.countFor(it) shouldBeGreaterThan 0 }
        }

        test("routine and trivial windows get the static evaluator, not playouts") {
            val states = candidateStates(3)
            val playerId = states.first().turnOrder.first()
            listOf(BudgetTier.TRIVIAL, BudgetTier.ROUTINE).forEach { tier ->
                val stub = RecordingPlayouts { 0.9 }
                val scores = RolloutCandidateEvaluator(stub, constantEvaluator(3.5))
                    .scoreAll(states.first(), states, playerId, budget(tier, 64))
                stub.calls.size shouldBe 0
                scores.forEach { it shouldBe 3.5 }
            }
        }

        test("a critical decision looks further ahead than a normal one") {
            val states = candidateStates(2)
            val playerId = states.first().turnOrder.first()
            val horizons = mutableListOf<Int>()
            val recorder = Playouts { _, _, _, horizon, _ -> horizons += horizon; 0.5 }

            RolloutCandidateEvaluator(recorder, zeroEvaluator())
                .scoreAll(states.first(), states, playerId, budget(BudgetTier.NORMAL, 4))
            val normal = horizons.distinct().single()
            horizons.clear()

            RolloutCandidateEvaluator(recorder, zeroEvaluator())
                .scoreAll(states.first(), states, playerId, budget(BudgetTier.CRITICAL, 4))
            horizons.distinct().single() shouldBeGreaterThan normal
        }
    }

    private fun zeroEvaluator() = BoardEvaluator { _, _, _ -> 0.0 }
    private fun constantEvaluator(value: Double) = BoardEvaluator { _, _, _ -> value }
}
