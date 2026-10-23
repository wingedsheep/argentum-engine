package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Bookwurm (STX #123) — {7}{G} Creature — Wurm, 7/7.
 *
 *   {2}{G}: Put this card from your graveyard into your library third from the top.
 *
 * Covers the graveyard-zone activation landing the card at the third position, and the ruling
 * that with fewer than two cards in the library it goes to the bottom.
 */
class BookwurmScenarioTest : ScenarioTestBase() {

    private fun activateFromGraveyard(game: TestGame) {
        val wurm = game.findCardsInGraveyard(1, "Bookwurm").single()
        val abilityId = cardRegistry.getCard("Bookwurm")!!.activatedAbilities.first().id
        val activation = game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = wurm, abilityId = abilityId)
        )
        withClue("activation from the graveyard should succeed: ${activation.error}") {
            activation.error shouldBe null
        }
        game.resolveStack()
    }

    init {
        context("Bookwurm") {

            test("{2}{G}: puts it into the library third from the top") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Bookwurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findCardsInGraveyard(1, "Bookwurm").single()
                activateFromGraveyard(game)

                withClue("Bookwurm left the graveyard and sits third from the top") {
                    game.isInGraveyard(1, "Bookwurm") shouldBe false
                    val library = game.state.getLibrary(game.player1Id)
                    library.size shouldBe 5
                    library[2] shouldBe wurm
                }
            }

            test("with only one card in the library it goes to the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Bookwurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findCardsInGraveyard(1, "Bookwurm").single()
                activateFromGraveyard(game)

                val library = game.state.getLibrary(game.player1Id)
                library.size shouldBe 2
                library.last() shouldBe wurm
            }
        }
    }
}
