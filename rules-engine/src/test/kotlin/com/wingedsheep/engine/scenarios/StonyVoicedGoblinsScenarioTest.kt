package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stony-Voiced Goblins (HOB) — {1}{B} Creature — Goblin Bard 1/1.
 * "When this creature enters, each opponent discards a card."
 *
 * The discard is the opponent's choice and hits only opponents — the controller's own hand must
 * be untouched, and the discarded card has to land in the discarding player's graveyard.
 */
class StonyVoicedGoblinsScenarioTest : ScenarioTestBase() {

    init {
        context("Stony-Voiced Goblins") {

            test("its ETB makes each opponent discard a card, sparing the controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stony-Voiced Goblins")
                    .withCardInHand(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stony-Voiced Goblins").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the opponent is asked which card to discard") {
                    (decision is SelectCardsDecision) shouldBe true
                    decision!!.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf((decision as SelectCardsDecision).options.first()))
                    .error shouldBe null
                game.resolveStack()

                withClue("the opponent discarded exactly one card") {
                    game.handSize(2) shouldBe 1
                    game.graveyardSize(2) shouldBe 1
                }
                withClue("the controller's own hand is untouched") {
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Centaur Courser") shouldBe true
                }
                withClue("the Goblins resolved onto the battlefield as a 1/1") {
                    val goblins = game.findPermanent("Stony-Voiced Goblins")!!
                    game.state.projectedState.getPower(goblins) shouldBe 1
                    game.state.projectedState.getToughness(goblins) shouldBe 1
                }
            }

            test("an opponent with an empty hand simply discards nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stony-Voiced Goblins")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stony-Voiced Goblins").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("no discard decision when there is nothing to discard") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe false
                }
                withClue("the Goblins still entered the battlefield") {
                    game.isOnBattlefield("Stony-Voiced Goblins") shouldBe true
                    game.graveyardSize(2) shouldBe 0
                }
            }
        }
    }
}
