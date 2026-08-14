package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Pollen-Shield Hare. */
class PollenShieldHareScenarioTest : ScenarioTestBase() {

    init {
        context("Pollen-Shield Hare — the anthem hits tokens only") {
            test("Rat tokens get +1/+1 while the Hare and a nontoken 2/2 do not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pollen-Shield Hare", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Ratcatcher Trainee")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val trainee = game.findCardsInHand(1, "Ratcatcher Trainee").first()
                game.execute(CastSpell(game.player1Id, trainee, emptyList(), faceIndex = 0))
                    .isSuccess shouldBe true
                game.resolveStack()

                val rat = game.findPermanent("Rat Token")!!
                withClue("a 1/1 token becomes 2/2") {
                    game.state.projectedState.getPower(rat) shouldBe 2
                    game.state.projectedState.getToughness(rat) shouldBe 2
                }

                val hare = game.findPermanent("Pollen-Shield Hare")!!
                withClue("the Hare is not a token, so it doesn't pump itself") {
                    game.state.projectedState.getPower(hare) shouldBe 2
                    game.state.projectedState.getToughness(hare) shouldBe 2
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("a nontoken creature you control is unaffected") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }

            test("Hare Raising grants vigilance and +X/+X for the creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pollen-Shield Hare")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = game.findCardsInHand(1, "Pollen-Shield Hare").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Permanent(bears)), faceIndex = 0)
                ).isSuccess shouldBe true
                game.resolveStack()

                withClue("two creatures you control → +2/+2 on a 2/2") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                withClue("…and vigilance") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
