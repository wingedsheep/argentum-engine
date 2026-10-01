package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nadu, Winged Wisdom (MH3) — creatures you control have "Whenever this creature becomes the target
 * of a spell or ability, reveal the top card of your library. If it's a land card, put it onto the
 * battlefield. Otherwise, put it into your hand. This ability triggers only twice each turn."
 */
class NaduWingedWisdomScenarioTest : ScenarioTestBase() {

    init {
        fun game() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Nadu, Winged Wisdom")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardsInHand(1, "Giant Growth", 4)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardInLibrary(1, "Island") // top: a land → battlefield
            .withCardInLibrary(1, "Centaur Courser") // a nonland → hand
            .withCardInLibrary(1, "Swamp") // never revealed by Nadu (cap spent)
            .withCardInLibrary(1, "Craw Wurm")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.grow(name: String) {
            castSpell(1, "Giant Growth", targetId = findPermanent(name)!!).error shouldBe null
            resolveStack()
        }

        test("a land goes onto the battlefield, a nonland into hand, and the third target triggers nothing") {
            val game = game()

            game.grow("Nadu, Winged Wisdom")
            withClue("Nadu has its own ability; the revealed Island enters") {
                game.findPermanents("Island").size shouldBe 1
            }

            game.grow("Nadu, Winged Wisdom")
            withClue("the revealed Centaur Courser goes to hand") { game.isInHand(1, "Centaur Courser") shouldBe true }

            val library = game.librarySize(1)
            game.grow("Nadu, Winged Wisdom")
            withClue("the ability triggers only twice each turn") {
                game.librarySize(1) shouldBe library
                game.findPermanents("Swamp").size shouldBe 0
            }
        }

        test("each creature counts its own two triggers") {
            val game = game()
            game.grow("Nadu, Winged Wisdom")
            game.grow("Nadu, Winged Wisdom")

            game.grow("Grizzly Bears")
            withClue("the Bears' copy of the ability has not triggered yet this turn") {
                game.findPermanents("Swamp").size shouldBe 1
            }
        }
    }
}
