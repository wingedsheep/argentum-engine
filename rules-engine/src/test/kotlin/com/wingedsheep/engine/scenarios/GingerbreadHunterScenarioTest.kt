package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Gingerbread Hunter // Puny Snack. */
class GingerbreadHunterScenarioTest : ScenarioTestBase() {

    init {
        context("Gingerbread Hunter // Puny Snack") {
            test("the creature face is a 5/5 that makes a Food on entry") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gingerbread Hunter")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gingerbread Hunter").error shouldBe null
                game.resolveStack()

                val hunter = game.findPermanent("Gingerbread Hunter")!!
                game.state.projectedState.getPower(hunter) shouldBe 5
                game.state.projectedState.getToughness(hunter) shouldBe 5
                withClue("the ETB made exactly one Food") {
                    game.findAllPermanents("Food").size shouldBe 1
                }
            }

            test("Puny Snack's -2/-2 kills a 2/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gingerbread Hunter")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = game.findCardsInHand(1, "Gingerbread Hunter").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Permanent(bears)), faceIndex = 0)
                ).isSuccess shouldBe true
                game.resolveStack()

                withClue("a 2/2 hit by -2/-2 dies to state-based actions") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("the Adventure card is exiled, castable as the Giant later") {
                    game.isInExile(1, "Gingerbread Hunter") shouldBe true
                }
            }
        }
    }
}
