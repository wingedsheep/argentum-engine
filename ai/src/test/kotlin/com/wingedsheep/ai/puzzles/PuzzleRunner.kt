package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/** How one agent did on one puzzle. [failure] is null exactly when [passed] is true. */
data class PuzzleResult(
    val puzzle: AiPuzzle,
    val passed: Boolean,
    /** [PuzzleMove.describe] of the chosen move, or why no move could be obtained. */
    val move: String,
    val failure: String?,
)

/**
 * Builds a puzzle position, asks an [AiProfile] for one move, and scores it.
 *
 * Takes the registry and scenario factory rather than extending [ScenarioTestBase] itself: the
 * builder is an inner class, so it needs a spec instance to exist, and the runner wants to be
 * callable from more than one spec (the always-on suite and the benchmark-gated report).
 *
 * A fresh [AIPlayer] per puzzle, never a shared one: `GameSimulator.isResolving` and
 * `decisionResolver` are mutable instance state.
 */
class PuzzleRunner(
    private val registry: CardRegistry,
    private val newScenario: () -> ScenarioTestBase.ScenarioBuilder,
) {
    private val processor = ActionProcessor(registry)

    fun run(puzzle: AiPuzzle, profile: AiProfile): PuzzleResult {
        val game = try {
            // `ScenarioBuilder` seeds itself from `System.nanoTime()` so repeated builds vary coin
            // flips. A CI gate cannot be seeded from the clock — pin it, exactly as the arena pins
            // `GameConfig.seed`.
            puzzle.position(newScenario().withRngSeed(PUZZLE_SEED))
        } catch (e: Throwable) {
            return PuzzleResult(puzzle, false, "—", "position failed to build: ${e.message}")
        }

        stockLibraries(game)

        val state = game.state
        val aiId = game.seatId(puzzle.aiSeat)
        val opponentId = game.seatId(if (puzzle.aiSeat == 1) 2 else 1)

        // A position where the AI is not the one to move scores whatever the first branch of the
        // check happens to say — which is worse than no puzzle at all. Fail loudly instead.
        state.pendingDecision?.let {
            return PuzzleResult(puzzle, false, "—", "position paused on ${it::class.simpleName}")
        }
        if (state.priorityPlayerId != aiId) {
            return PuzzleResult(
                puzzle, false, "—",
                "position gives priority to ${state.priorityPlayerId} at ${state.phase}/${state.step}, " +
                    "not the AI (seat ${puzzle.aiSeat})",
            )
        }

        val ai = AIPlayer.create(registry, aiId, profile)
        val action = try {
            ai.chooseAction(state)
        } catch (e: Throwable) {
            return PuzzleResult(puzzle, false, "—", "AI threw ${e::class.simpleName}: ${e.message}")
        }
        val move = PuzzleMove(state, aiId, opponentId, action)

        // The arena found the AI proposing ~0.9 illegal actions per game. A puzzle that "passes"
        // on an action the engine would reject is measuring nothing, so legality is part of the bar.
        processor.process(state, action).result.error?.let { error ->
            return PuzzleResult(puzzle, false, move.describe(), "engine rejected the move: $error")
        }

        return try {
            puzzle.check(move)
            PuzzleResult(puzzle, true, move.describe(), null)
        } catch (e: AssertionError) {
            PuzzleResult(puzzle, false, move.describe(), e.message ?: "assertion failed")
        }
    }

    /**
     * Give every seat a library, because `ScenarioBuilder.withPlayers` leaves it empty.
     *
     * Invisible to a 1-ply agent and fatal to a searching one, which is why it went unnoticed until
     * Phase 7. `BoardFeatures` never reads library size and a single simulated action never crosses
     * a draw step, so `v0` cannot tell the difference — but a rollout plays two turns forward, hits
     * the draw step with nothing to draw, and every playout ends in a decking race decided by whose
     * draw step comes first (CR 104.3c). A puzzle answered that way measures the harness.
     *
     * **A vanilla six-drop, and emphatically not a basic land.** The original filler was `Forest`,
     * on the reasoning that "a land is the most inert card in Magic, so a stocked library adds a
     * clock and nothing else". That stopped being true the day
     * [com.wingedsheep.ai.engine.AiProfile.landDropIsNotCardLoss] shipped, which stops *counting*
     * one land per unused land drop — a simplification, and a land in hand is of course a card with
     * value (`CardAdvantage.heldCardCount` now records the limitation). With an all-Forest library
     * the consequence is total: drawing a card draws a land, and a lone earmarked land counts as an
     * **empty hand**. So every card the AI drew inside a simulation stepped straight off the topdeck
     * cliff, and any spell that drew one was charged for it. Measured on `timing-05`: casting Opt scored −0.45 against passing on
     * `production` and **−4.45** on the same agent with nothing changed but that flag, which is four
     * points of pure harness. Its `cashCantripsInTheEndStep` window covers 1.5 of that and so looked
     * like a term that did not work.
     *
     * `Craw Wurm` is the replacement because it is inert to the *evaluator* rather than inert in
     * Magic: no ETB, no keywords, no activated ability, and at six mana it is out of reach in
     * essentially every position here. Re-measured across all 34 profiles in
     * [PuzzleComparisonBenchmark], the swap moves **four verdicts, all upward**, and leaves
     * `production` and every promotion baseline — and therefore `PuzzleSuiteTest.KNOWN_FAILURES` —
     * untouched. All four are searching agents, which is the same story from the other side: an
     * all-basic-land library is a distribution only a rollout is deep enough to notice.
     *
     * The general rule this cost us, worth stating once: **the filler must be a card no evaluator
     * term special-cases.** "Inert in Magic" is not the same property, and the gap between them is
     * silent.
     *
     * Deep enough that no horizon reaches the bottom.
     */
    private fun stockLibraries(game: ScenarioTestBase.TestGame) {
        val filler = registry.getCard(LIBRARY_FILLER) ?: return
        var state = game.state
        var counter = 0
        for (playerId in state.turnOrder) {
            val zone = ZoneKey(playerId, Zone.LIBRARY)
            if (state.zones[zone]?.isNotEmpty() == true) continue
            repeat(LIBRARY_SIZE) {
                val cardId = EntityId.of("puzzle-library-${playerId.value}-${counter++}")
                state = state
                    .withEntity(cardId, CardEntityFactory.create(filler, playerId))
                    .addToZone(zone, cardId)
            }
        }
        game.state = state
    }

    fun runAll(puzzles: List<AiPuzzle>, profile: AiProfile): List<PuzzleResult> =
        puzzles.map { run(it, profile) }

    private companion object {
        /** Same date-stamp convention as the arena's `ArenaConfig.DEFAULT_SEED`. */
        const val PUZZLE_SEED = 20260727L

        /** Deeper than any rollout horizon can reach, and shorter than a real deck. */
        const val LIBRARY_SIZE = 30

        /** See [stockLibraries]. A card no evaluation term special-cases — in particular, not a land. */
        const val LIBRARY_FILLER = "Craw Wurm"
    }
}
