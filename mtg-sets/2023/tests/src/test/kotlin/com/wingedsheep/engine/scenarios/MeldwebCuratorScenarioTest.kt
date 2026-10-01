package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Meldweb Curator (ONE #59) — {3}{U} Phyrexian Wizard, 3/4.
 *
 * "When this creature enters, put up to one target instant or sorcery card from your graveyard
 *  on top of your library."
 */
class MeldwebCuratorScenarioTest : ScenarioTestBase() {

    init {
        context("Meldweb Curator enters-the-battlefield trigger") {

            test("puts an instant from your graveyard on top of your library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Meldweb Curator")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Rebellious Strike")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Meldweb Curator").error shouldBe null
                game.resolveStack()

                val strike = game.findCardsInGraveyard(1, "Rebellious Strike").first()
                game.selectTargets(listOf(strike)).error shouldBe null
                game.resolveStack()

                withClue("Rebellious Strike leaves the graveyard") {
                    game.findCardsInGraveyard(1, "Rebellious Strike").size shouldBe 0
                }
                withClue("Rebellious Strike is on top of the library") {
                    game.state.getLibrary(game.player1Id).first() shouldBe strike
                }
                game.isOnBattlefield("Meldweb Curator") shouldBe true
            }

            test("a creature card is not a legal target; the trigger may choose nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Meldweb Curator")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Meldweb Curator").error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                withClue("A creature card is not an instant or sorcery") {
                    (game.selectTargets(listOf(bears)).error != null) shouldBe true
                }
                game.skipTargets().error shouldBe null
                game.resolveStack()

                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                game.isOnBattlefield("Meldweb Curator") shouldBe true
            }
        }
    }
}
