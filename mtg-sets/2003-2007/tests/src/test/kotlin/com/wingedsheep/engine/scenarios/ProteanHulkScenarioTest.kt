package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Protean Hulk — "When this creature dies, search your library for any number of creature cards
 * with total mana value 6 or less, put them onto the battlefield, then shuffle."
 */
class ProteanHulkScenarioTest : ScenarioTestBase() {

    private fun hulkDies(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Protean Hulk")
            .withCardInHand(1, "Doom Blade")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInLibrary(1, "Centaur Courser") // MV 3
            .withCardInLibrary(1, "Grizzly Bears") // MV 2
            .withCardInLibrary(1, "Savannah Lions") // MV 1
            .withCardInLibrary(1, "Craw Wurm") // MV 6
            .withCardInLibrary(1, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val hulk = game.findPermanent("Protean Hulk")!!
        val cast = game.castSpell(1, "Doom Blade", hulk)
        withClue("Doom Blade should cast: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()

        withClue("Protean Hulk should be in the graveyard") {
            game.findCardsInGraveyard(1, "Protean Hulk").size shouldBe 1
        }
        withClue("The dies trigger should pause for the library search") {
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        }
        return game
    }

    init {
        context("Protean Hulk") {
            test("puts several creature cards with total mana value exactly 6 onto the battlefield") {
                val game = hulkDies()
                val picks = listOf("Centaur Courser", "Grizzly Bears", "Savannah Lions").map {
                    game.findCardsInLibrary(1, it).single()
                }
                val select = game.selectCards(picks)
                withClue("Selecting MV 3 + 2 + 1 should succeed: ${select.error}") { select.error shouldBe null }
                game.resolveStack()

                game.findPermanent("Centaur Courser") shouldBe picks[0]
                game.findPermanent("Grizzly Bears") shouldBe picks[1]
                game.findPermanent("Savannah Lions") shouldBe picks[2]
                withClue("Unchosen cards stay in the library") {
                    game.findCardsInLibrary(1, "Craw Wurm").size shouldBe 1
                    game.findCardsInLibrary(1, "Swamp").size shouldBe 1
                }
            }

            test("never puts more than 6 total mana value onto the battlefield") {
                val game = hulkDies()
                val wurm = game.findCardsInLibrary(1, "Craw Wurm").single()
                val lions = game.findCardsInLibrary(1, "Savannah Lions").single()
                game.selectCards(listOf(wurm, lions))
                game.resolveStack()

                withClue("Craw Wurm (MV 6) alone uses the whole budget") {
                    game.findPermanent("Craw Wurm") shouldBe wurm
                }
                withClue("Savannah Lions would push the total to 7, so it stays in the library") {
                    game.findPermanent("Savannah Lions") shouldBe null
                    game.findCardsInLibrary(1, "Savannah Lions").size shouldBe 1
                }
            }

            test("may find nothing") {
                val game = hulkDies()
                game.skipSelection()
                game.resolveStack()

                game.findPermanent("Centaur Courser") shouldBe null
                game.findCardsInLibrary(1, "Centaur Courser").size shouldBe 1
            }
        }
    }
}
