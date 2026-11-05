package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Gain control of target creature until end of turn. Untap that creature. It gains haste" and its
 * riders — Threaten untapping *before* it steals, Goatnap's "if it's a Goat, it gets +3/+0" — are one
 * theft: the untap, the haste and the pump go to the creature we took, so the slot reads as the theft
 * (harmful to the creature's controller), not as a theft and a gift that disagree.
 */
class ThreatenFollowUpReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(id = "threaten", category = PuzzleCategory.PRIORITY_TIMING, expectation = "", aiSeat = 1, position = position, check = check),
            AiProfile.LIVE,
        )

    init {
        test("a theft's untap, haste and pump on the stolen creature read as part of the theft") {
            for (card in listOf("Goatnap", "Threaten", "Act of Treason", "Traitorous Blood")) {
                val script = cardRegistry.requireCard(card).script
                withClue("$card: the slot is the creature we take, so it reads harmful to its controller") {
                    TargetPolarityAnalyzer.forRequirements(script.spellEffect, script.targetRequirements) shouldBe listOf(TargetPolarity.HARMFUL)
                }
            }
        }
        for (card in listOf("Goatnap", "Threaten")) {
            test("$card steals the opponent's creature rather than untapping our own") {
                val result = decide({ s ->
                    s.withPlayers()
                        .withActivePlayer(1)
                        .withLifeTotal(2, 8)
                        .withLandsOnBattlefield(1, "Mountain", 4)
                        .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                        .withCardOnBattlefield(1, "Craw Wurm", tapped = true)
                        .withCardInHand(1, card)
                        .withCardOnBattlefield(2, "Hill Giant")
                        .apply { repeat(4) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Swamp") } }
                        .build()
                        .also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
                }) { shouldCast(card); shouldTarget("Hill Giant") }
                withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
            }
        }
    }
}
