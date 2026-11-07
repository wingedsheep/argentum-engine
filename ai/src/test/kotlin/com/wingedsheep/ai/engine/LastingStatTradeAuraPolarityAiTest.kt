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
 * An Aura whose static trades one stat for the other — Boon of Emrakul's "+3/-3", Maggot Therapy's
 * "+2/-2" — kills a small creature of the opponent's and turns a sturdy one of ours into a bigger
 * attacker. Its enchant slot reads FLEXIBLE: both sides stay eligible and the simulation picks.
 */
class LastingStatTradeAuraPolarityAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit) {
        val result = runner.run(
            AiPuzzle(
                id = "lasting-stat-trade-aura", category = PuzzleCategory.LAST_CHANCE,
                expectation = "Aim a +N/-M Aura where the trade pays off", aiSeat = 1,
                position = position, check = check,
            ),
            AiProfile.LIVE,
        )
        withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
    }

    init {
        test("a lasting +N/-M Aura reads flexible and is read fully") {
            for (name in listOf("Boon of Emrakul", "Maggot Therapy")) {
                val card = cardRegistry.getCard(name)!!
                withClue(name) {
                    TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.FLEXIBLE)
                    AiReadingAudit.read(card).readFully shouldBe true
                }
            }
        }

        test("Boon of Emrakul kills the opponent's Hill Giant rather than shrinking our Grizzly Bears") {
            decide({ s ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Boon of Emrakul")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            }) {
                shouldCast("Boon of Emrakul")
                shouldTarget("Hill Giant")
            }
        }
    }
}
