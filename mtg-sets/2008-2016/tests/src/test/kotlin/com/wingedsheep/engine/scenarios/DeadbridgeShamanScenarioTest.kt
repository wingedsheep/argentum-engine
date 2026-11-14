package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Deadbridge Shaman (ORI #91) — {2}{B} Creature — Elf Shaman, 3/1.
 *
 *   When this creature dies, target opponent discards a card.
 */
class DeadbridgeShamanScenarioTest : ScenarioTestBase() {

    init {
        context("Deadbridge Shaman") {

            test("when it dies, the opponent discards a card of their choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deadbridge Shaman")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shaman = game.findPermanent("Deadbridge Shaman")!!
                val bolt = game.castSpell(1, "Lightning Bolt", shaman)
                withClue("Bolt should be cast: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()

                withClue("Deadbridge Shaman died") { game.isInGraveyard(1, "Deadbridge Shaman") shouldBe true }

                // The lone opponent is auto-selected as the trigger's target; the opponent then picks the discard.
                val discard = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the opponent chooses what to discard") { discard.playerId shouldBe game.player2Id }
                val giant = game.findCardsInHand(2, "Hill Giant").single()
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.handSize(2) shouldBe 1
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInHand(2, "Grizzly Bears") shouldBe true
                withClue("the controller of the Shaman discards nothing") { game.handSize(1) shouldBe 0 }
            }
        }
    }
}
