package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gul Dukat, Colonial Prefect: at the beginning of each end step, each opponent loses life equal
 * to the life *that player* lost this turn, and Dukat's controller gains the total.
 */
class GulDukatColonialPrefectScenarioTest : ScenarioTestBase() {

    init {
        context("Gul Dukat, Colonial Prefect") {
            test("the opponent loses the life they lost this turn again, and you gain it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gul Dukat, Colonial Prefect")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("opponent loses the 3 they lost this turn") { game.getLifeTotal(2) shouldBe 14 }
                withClue("you gain the 3 lost this way") { game.getLifeTotal(1) shouldBe 23 }
            }

            test("it measures the opponent's loss, not yours") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gul Dukat, Colonial Prefect")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 17

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("opponent lost nothing this turn") { game.getLifeTotal(2) shouldBe 20 }
                withClue("so nothing is gained") { game.getLifeTotal(1) shouldBe 17 }
            }

            test("it triggers in the opponent's end step too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gul Dukat, Colonial Prefect")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // The opponent pings themself on their own turn.
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 14
                game.getLifeTotal(1) shouldBe 23
            }
        }
    }
}
