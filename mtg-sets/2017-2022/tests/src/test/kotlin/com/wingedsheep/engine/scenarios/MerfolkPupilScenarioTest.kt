package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Merfolk Pupil (J22 #15) — {1}{U} Creature — Merfolk Wizard, 1/1.
 *
 *   When this creature enters, draw a card, then discard a card.
 *   {1}{U}, Exile this card from your graveyard: Draw a card, then discard a card.
 *
 * Exercises both loots: the ETB trigger and the graveyard-activated exile-self ability.
 */
class MerfolkPupilScenarioTest : ScenarioTestBase() {

    init {
        context("Merfolk Pupil") {

            test("entering the battlefield draws a card, then discards a chosen card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Merfolk Pupil")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Merfolk Pupil").error shouldBe null
                game.resolveStack()

                val discard = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a discard selection; got ${game.getPendingDecision()}")
                withClue("the discard is chosen from a hand holding the drawn card too") {
                    discard.options.size shouldBe 2
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                withClue("the drawn card stays, the chosen card is discarded") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.handSize(1) shouldBe 1
                }
            }

            test("{1}{U}, exiling it from the graveyard, draws then discards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Merfolk Pupil")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pupil = game.findCardsInGraveyard(1, "Merfolk Pupil").single()
                val abilityId = cardRegistry.getCard("Merfolk Pupil")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = pupil, abilityId = abilityId)
                )
                withClue("activation from the graveyard should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                withClue("exiling the card is part of the cost") {
                    game.isInExile(1, "Merfolk Pupil") shouldBe true
                }
                game.resolveStack()

                game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a discard selection; got ${game.getPendingDecision()}")
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                withClue("drew Hill Giant and discarded Grizzly Bears") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
