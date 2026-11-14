package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Reclaim (EXO #120) — {G} Instant.
 *
 *   Put target card from your graveyard on top of your library.
 */
class ReclaimScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Reclaim")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(2, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Reclaim") {

            test("puts target card from your graveyard on top of your library") {
                val game = board()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                val cast = game.castSpellTargetingGraveyardCard(1, "Reclaim", 1, "Grizzly Bears")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears is now the top card of your library") {
                    game.state.getLibrary(game.player1Id).first() shouldBe bears
                    game.librarySize(1) shouldBe 3
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
                game.isInGraveyard(1, "Reclaim") shouldBe true
            }

            test("can't target a card in an opponent's graveyard") {
                val game = board()

                val cast = game.castSpellTargetingGraveyardCard(1, "Reclaim", 2, "Hill Giant")
                withClue("an opponent's graveyard card isn't a legal target") { cast.error shouldNotBe null }
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }
    }
}
