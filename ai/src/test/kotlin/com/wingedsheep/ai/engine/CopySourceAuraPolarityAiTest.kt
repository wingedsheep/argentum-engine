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
 * An Aura whose host is only a copy source — Infinite Reflection's "each other nontoken creature you
 * control becomes a copy of that creature" and "nontoken creatures you control enter as a copy of
 * enchanted creature". The host itself is untouched, and the best creature to copy may be on either
 * side, so the enchant slot reads FLEXIBLE and the simulation picks it.
 */
class CopySourceAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "copy-source-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Enchant the best creature to copy, whoever controls it", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("Infinite Reflection's enchant slot reads flexible and is read fully") {
            val card = cardRegistry.getCard("Infinite Reflection")!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.FLEXIBLE)
            AiReadingAudit.read(card).readFully shouldBe true
        }

        test("Infinite Reflection goes on the opponent's Craw Wurm, turning our small creatures into Wurms") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInHand(1, "Infinite Reflection")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .build()
            }) {
                shouldCast("Infinite Reflection")
                shouldTarget("Craw Wurm")
            }
        }
    }
}
