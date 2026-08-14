package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Song of Totentanz. */
class SongOfTotentanzScenarioTest : ScenarioTestBase() {

    init {
        context("Song of Totentanz — X Rats, then haste for the team") {
            test("X=2 makes two Rats and every creature you control gains haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Song of Totentanz")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castXSpell(1, "Song of Totentanz", 2).error shouldBe null
                game.resolveStack()

                val rats = game.findAllPermanents("Rat Token")
                withClue("X = 2, so two Rat tokens") { rats.size shouldBe 2 }

                withClue("the Rats were created before the haste grant, so they get haste too") {
                    rats.forEach { game.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe true }
                }
                withClue("creatures you already controlled gain haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
                withClue("an opponent's creature is untouched") {
                    game.state.projectedState.hasKeyword(wurm, Keyword.HASTE) shouldBe false
                }
            }

            test("X=0 makes no Rats but still grants haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Song of Totentanz")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castXSpell(1, "Song of Totentanz", 0).error shouldBe null
                game.resolveStack()

                game.findAllPermanents("Rat Token").size shouldBe 0
                game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
            }
        }
    }
}
