package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for Misleading Motes and Into the Fae Court.
 *
 * Misleading Motes must give the top-or-bottom decision to the target's owner. Into the Fae Court
 * must perform both halves in printed order and create the WOE Faerie token rather than a generic
 * creature. The token helper's full static-ability tree is additionally pinned by the card snapshot.
 */
class WoeMotesAndFaeCourtScenarioTest : ScenarioTestBase() {

    init {
        context("Misleading Motes") {
            test("the targeted creature's owner chooses to put it on the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Misleading Motes")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Misleading Motes", bears).error shouldBe null
                game.resolveStack()

                withClue("the targeted creature's owner gets the top-or-bottom decision") {
                    game.getPendingDecision()!!.playerId shouldBe game.player2Id
                }
                game.submitDecision(OptionChosenResponse(game.getPendingDecision()!!.id, 1))
                game.resolveStack()

                withClue("the creature is on the bottom, below the existing Forest") {
                    game.state.getLibrary(game.player2Id).last() shouldBe bears
                }
            }
        }

        context("Into the Fae Court") {
            test("draws three cards and creates the complete WOE Faerie token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Into the Fae Court")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Into the Fae Court").error shouldBe null
                game.resolveStack()

                withClue("all three cards were drawn") {
                    game.handSize(1) shouldBe 3
                    game.state.getLibrary(game.player1Id).size shouldBe 0
                }

                val faerie = game.findPermanent("Faerie Token")!!
                withClue("the token is a 1/1 flyer") {
                    game.state.projectedState.getPower(faerie) shouldBe 1
                    game.state.projectedState.getToughness(faerie) shouldBe 1
                    game.state.projectedState.hasKeyword(faerie, Keyword.FLYING) shouldBe true
                }
            }

        }
    }
}
