package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Commune with Evil (DSK #87).
 *
 * Commune with Evil — {2}{B} Sorcery
 *   "Look at the top four cards of your library. Put one of them into your hand and the rest
 *    into your graveyard. You gain 3 life."
 *
 * Verifies the look-at-top-four / keep-one-to-hand / rest-to-graveyard pipeline and the
 * +3 life gain.
 */
class CommuneWithEvilScenarioTest : ScenarioTestBase() {

    init {
        context("Commune with Evil") {

            test("keep one of the top four in hand, the other three to graveyard, gain 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Commune with Evil")
                    .withLandsOnBattlefield(1, "Swamp", 3) // {2}{B}
                    // Four distinct named cards on top so we can identify what moved where.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                val handCountBefore = game.findCardsInHand(1, "Commune with Evil").size

                val cast = game.castSpell(1, "Commune with Evil")
                withClue("Casting Commune with Evil should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // Pick one of the four to keep in hand.
                val decision = game.state.pendingDecision
                withClue("A selection decision should be presented for which card to keep") {
                    (decision != null) shouldBe true
                }
                val kept = game.findCardsInLibrary(1, "Grizzly Bears").firstOrNull()
                    ?: error("expected Grizzly Bears among the looked-at cards")
                game.selectCards(listOf(kept))
                game.resolveStack()

                withClue("The chosen card ends up in hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("The other three looked-at cards go to the graveyard") {
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 1
                    game.findCardsInGraveyard(1, "Plains").size shouldBe 1
                    game.findCardsInGraveyard(1, "Mountain").size shouldBe 1
                }
                withClue("Controller gains 3 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 3
                }
                // Sanity: the sorcery itself went to the graveyard, not back to hand.
                handCountBefore shouldBe 1
                game.findCardsInGraveyard(1, "Commune with Evil").size shouldBe 1
            }
        }
    }
}
