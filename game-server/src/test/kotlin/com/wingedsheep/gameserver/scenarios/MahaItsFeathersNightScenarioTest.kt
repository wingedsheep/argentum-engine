package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Maha, Its Feathers Night.
 *
 * Card reference:
 * - Maha, Its Feathers Night ({3}{B}{B}): Legendary Creature — Elemental Bird 6/5
 *   Flying, trample
 *   Ward—Discard a card.
 *   Creatures your opponents control have base toughness 1.
 */
class MahaItsFeathersNightScenarioTest : ScenarioTestBase() {

    init {
        context("Maha base toughness static ability") {

            test("sets opponent creatures' base toughness to 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Maha, Its Feathers Night")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!

                // Glory Seeker should have base toughness set to 1 (2/1 instead of 2/2)
                val clientState = game.getClientState(1)
                val seekerInfo = clientState.cards[seekerId]
                withClue("Glory Seeker should be 2/1 with base toughness set to 1") {
                    seekerInfo shouldNotBe null
                    seekerInfo!!.power shouldBe 2
                    seekerInfo.toughness shouldBe 1
                }
            }

            test("does not affect controller's own creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Maha, Its Feathers Night")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 controlled by Maha's controller
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!

                // Glory Seeker controlled by the same player should remain 2/2
                val clientState = game.getClientState(1)
                val seekerInfo = clientState.cards[seekerId]
                withClue("Glory Seeker (own creature) should remain 2/2") {
                    seekerInfo shouldNotBe null
                    seekerInfo!!.power shouldBe 2
                    seekerInfo.toughness shouldBe 2
                }
            }

            test("only changes toughness, not power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Maha, Its Feathers Night")
                    .withCardOnBattlefield(2, "Battering Craghorn") // 3/1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val craghornId = game.findPermanent("Battering Craghorn")!!

                // Battering Craghorn base is 3/1, toughness set to 1 means still 3/1
                val clientState = game.getClientState(1)
                val craghornInfo = clientState.cards[craghornId]
                withClue("Battering Craghorn should be 3/1 (power unchanged, toughness set to 1)") {
                    craghornInfo shouldNotBe null
                    craghornInfo!!.power shouldBe 3
                    craghornInfo.toughness shouldBe 1
                }
            }
        }
    }
}
