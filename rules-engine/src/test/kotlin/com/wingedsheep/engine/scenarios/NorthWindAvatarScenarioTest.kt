package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for North Wind Avatar (TMT):
 * "Flying
 *  When this creature enters, if you cast it, you may put a card you own from outside the game
 *  into your hand."
 *
 * A wish gated on the intervening "if you cast it" clause. Unlike the Burning Wish cycle it has no
 * "reveal that card" clause — the fetched card goes to hand without being revealed — and it fetches
 * *any* card (not a typed one). "Outside the game" is the private SIDEBOARD.
 */
class NorthWindAvatarScenarioTest : ScenarioTestBase() {

    init {
        context("North Wind Avatar's cast-ETB wish") {
            test("casting it offers any sideboard card and puts the chosen one into hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "North Wind Avatar")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInSideboard(1, "Grizzly Bears") // any card is eligible
                    .withCardInSideboard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "North Wind Avatar")
                withClue("North Wind Avatar should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("The cast-ETB wish should pause for a sideboard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                decision.shouldNotBeNull()
                val offered = decision.cardInfo!!
                withClue("Any card you own from outside the game is offered (not just one type)") {
                    offered.values.any { it.name == "Grizzly Bears" } shouldBe true
                    offered.values.any { it.name == "Lightning Bolt" } shouldBe true
                }

                val boltId = offered.entries.first { it.value.name == "Lightning Bolt" }.key
                game.selectCards(listOf(boltId))

                withClue("The chosen card is now in hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                }
                withClue("...and has left the sideboard") {
                    game.isInSideboard(1, "Lightning Bolt") shouldBe false
                }
                withClue("North Wind Avatar itself is on the battlefield") {
                    game.findPermanent("North Wind Avatar").shouldNotBeNull()
                }
            }
        }
    }
}
