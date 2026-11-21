package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An Arrest-style lock whose controller can cash it in — Krasis Incubation's "{1}{G}{U}, Return
 * this Aura to its owner's hand: Put two +1/+1 counters on enchanted creature." The activation's
 * cost takes the Aura off the board, so the counters are a parting gift that lifts the curse, a
 * consolation like a granted ability: beside "can't attack or block" the Aura is still a curse,
 * and goes on the opponent's creature.
 */
class SelfBounceReleaseAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "self-bounce-release-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Aim a lock-down Aura with a self-bounce payoff at the opponent's creature", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("Krasis Incubation reads as a curse, fully") {
            val incubation = cardRegistry.getCard(INCUBATION)!!
            TargetPolarityAnalyzer.forSpell(incubation) shouldBe listOf(TargetPolarity.HARMFUL)
            withClue(AiReadingAudit.read(incubation).toString()) {
                AiReadingAudit.read(incubation).readFully shouldBe true
            }
        }

        test("Krasis Incubation goes on the opponent's creature, not on our own") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, INCUBATION)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast(INCUBATION)
                shouldTarget("Hill Giant")
            }
        }
    }

    private companion object {
        const val INCUBATION = "Krasis Incubation"
    }
}
