package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Queen Marchesa (CN2 #78) — {1}{R}{W}{B} 3/3 deathtouch, haste.
 * "When Queen Marchesa enters, you become the monarch. At the beginning of your upkeep, if an
 * opponent is the monarch, create a 1/1 black Assassin creature token with deathtouch and haste."
 */
class QueenMarchesaScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Marchesa", "Rival")
        .withCardInHand(1, "Queen Marchesa")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(2, "Grizzly Bears")
        .withCardInLibrary(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Queen Marchesa") {

            test("entering makes you the monarch") {
                val game = game()
                game.castSpell(1, "Queen Marchesa").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Queen Marchesa") shouldBe true
                game.state.monarch shouldBe game.player1Id
            }

            test("upkeep makes an Assassin while an opponent is the monarch") {
                val game = scenario()
                    .withPlayers("Marchesa", "Rival")
                    .withCardOnBattlefield(1, "Queen Marchesa")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                game.state = game.state.copy(monarch = game.player2Id)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val assassins = game.findPermanents("Assassin Token")
                withClue("One 1/1 Assassin with deathtouch and haste") {
                    assassins.size shouldBe 1
                    game.state.projectedState.getPower(assassins.single()) shouldBe 1
                    game.state.projectedState.hasKeyword(assassins.single(), Keyword.DEATHTOUCH) shouldBe true
                    game.state.projectedState.hasKeyword(assassins.single(), Keyword.HASTE) shouldBe true
                }
            }

            test("no Assassin while you are the monarch, or while there is none") {
                for (monarch in listOf<Int?>(1, null)) {
                    val game = scenario()
                        .withPlayers("Marchesa", "Rival")
                        .withCardOnBattlefield(1, "Queen Marchesa")
                        .withCardInLibrary(1, "Grizzly Bears")
                        .withCardInLibrary(1, "Grizzly Bears")
                        .withCardInLibrary(2, "Grizzly Bears")
                        .withCardInLibrary(2, "Grizzly Bears")
                        .withActivePlayer(2)
                        .inPhase(Phase.ENDING, Step.END)
                        .build()
                    game.state = game.state.copy(monarch = monarch?.let { game.player1Id })

                    game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                    game.resolveStack()

                    withClue("monarch = $monarch") {
                        game.findPermanents("Assassin Token").size shouldBe 0
                    }
                }
            }
        }
    }
}
