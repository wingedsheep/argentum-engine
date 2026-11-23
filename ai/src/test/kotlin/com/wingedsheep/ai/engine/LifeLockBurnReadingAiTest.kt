package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Players can't gain life this turn. Damage can't be prevented this turn. Skullcrack deals 3
 * damage to target player or planeswalker." The life-gain lock rides on the burn beside it, so the
 * AI times the card as the burn spell it is.
 */
class LifeLockBurnReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("the life-gain lock beside burn is timed by the burn") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Skullcrack"))
            withClue("Skullcrack: $reading") { reading.readFully shouldBe true }
        }

        test("Skullcrack goes to the opponent's face for lethal") {
            val result = runner.run(
                AiPuzzle(
                    id = "skullcrack-finisher",
                    category = PuzzleCategory.LETHAL_DETECTION,
                    expectation = "",
                    aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withActivePlayer(1)
                            .withLifeTotal(2, 3)
                            .withLandsOnBattlefield(1, "Mountain", 2)
                            .withCardInHand(1, "Skullcrack")
                            .apply { repeat(4) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }
                            .build()
                            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
                    },
                    check = { shouldCast("Skullcrack"); shouldTargetOpponentFace() },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }
    }
}
