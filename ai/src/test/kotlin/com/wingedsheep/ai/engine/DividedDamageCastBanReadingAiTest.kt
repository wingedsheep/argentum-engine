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
 * "Aurelia's Fury deals X damage divided as you choose among any number of targets. Tap each
 * creature dealt damage this way. Players dealt damage this way can't cast noncreature spells this
 * turn." The cast ban binds only the players the division reached, so it rides on the burn and the
 * AI times the card as the burn spell it is.
 */
class DividedDamageCastBanReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    init {
        test("the cast ban on players dealt damage this way is timed by the divided damage") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Aurelia's Fury"))
            withClue("Aurelia's Fury: $reading") { reading.readFully shouldBe true }
        }

        test("Aurelia's Fury goes to the opponent's face for lethal") {
            val result = runner.run(
                AiPuzzle(
                    id = "aurelias-fury-finisher",
                    category = PuzzleCategory.LETHAL_DETECTION,
                    expectation = "",
                    aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withActivePlayer(1)
                            .withLifeTotal(2, 3)
                            .withLandsOnBattlefield(1, "Mountain", 4)
                            .withLandsOnBattlefield(1, "Plains", 1)
                            .withCardInHand(1, "Aurelia's Fury")
                            .apply { repeat(4) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }
                            .build()
                            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
                    },
                    check = { shouldCast("Aurelia's Fury"); shouldTargetOpponentFace() },
                ),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move} — ${result.failure}") { result.passed shouldBe true }
        }
    }
}
