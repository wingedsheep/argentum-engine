package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Intangible Virtue. */
class IntangibleVirtueScenarioTest : ScenarioTestBase() {

    init {
        context("Intangible Virtue — the anthem hits creature tokens only") {
            test("Spirit tokens get +1/+1 and vigilance while a nontoken creature gets neither") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Intangible Virtue")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Midnight Haunting")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val haunting = game.findCardsInHand(1, "Midnight Haunting").first()
                game.execute(CastSpell(game.player1Id, haunting, emptyList(), faceIndex = 0))
                    .isSuccess shouldBe true
                game.resolveStack()

                val spirit = game.findPermanent("Spirit Token")!!
                withClue("a 1/1 creature token becomes 2/2") {
                    game.state.projectedState.getPower(spirit) shouldBe 2
                    game.state.projectedState.getToughness(spirit) shouldBe 2
                }
                withClue("…and has vigilance") {
                    game.state.projectedState.hasKeyword(spirit, Keyword.VIGILANCE) shouldBe true
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("a nontoken creature you control keeps its printed P/T") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("a nontoken creature you control does not gain vigilance") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }
            }

            test("a creature token an opponent controls is unaffected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Intangible Virtue")
                    .withCardInHand(2, "Midnight Haunting")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val haunting = game.findCardsInHand(2, "Midnight Haunting").first()
                game.execute(CastSpell(game.player2Id, haunting, emptyList(), faceIndex = 0))
                    .isSuccess shouldBe true
                game.resolveStack()

                val spirit = game.findPermanent("Spirit Token")!!
                withClue("the anthem is scoped to tokens *you* control") {
                    game.state.projectedState.getPower(spirit) shouldBe 1
                    game.state.projectedState.getToughness(spirit) shouldBe 1
                    game.state.projectedState.hasKeyword(spirit, Keyword.VIGILANCE) shouldBe false
                }
            }
        }
    }
}
