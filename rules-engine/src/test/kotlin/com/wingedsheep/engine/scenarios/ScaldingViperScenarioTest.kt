package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Scalding Viper. */
class ScaldingViperScenarioTest : ScenarioTestBase() {

    init {
        context("Scalding Viper — 1 damage on an opponent's cheap spell") {
            fun viperGame(opponentCard: String) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Scalding Viper", summoningSickness = false)
                .withCardInHand(2, opponentCard)
                .withLandsOnBattlefield(2, "Forest", 6)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("an opponent's mana value 2 spell pings them for 1") {
                val game = viperGame("Grizzly Bears")

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("the caster took 1 damage") { game.getLifeTotal(2) shouldBe 19 }
                withClue("the Viper's controller is untouched") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("a mana value 6 spell is above the threshold — no damage") {
                val game = viperGame("Craw Wurm")

                game.castSpell(2, "Craw Wurm").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("your own cheap spell doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Scalding Viper", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("Steam Clean bounces a nonland permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scalding Viper")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = game.findCardsInHand(1, "Scalding Viper").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Permanent(bears)), faceIndex = 0)
                ).isSuccess shouldBe true
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Scalding Viper") shouldBe true
            }
        }
    }
}
