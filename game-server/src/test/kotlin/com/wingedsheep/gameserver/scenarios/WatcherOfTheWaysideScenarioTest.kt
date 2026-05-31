package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Watcher of the Wayside (TDM #249) — {3} Artifact Creature — Golem, 3/2.
 *
 * "When this creature enters, target player mills two cards. You gain 2 life."
 *
 * Verifies the enters trigger: the targeted player mills two, and the controller gains 2 life
 * unconditionally (the life gain is a separate clause, independent of the mill).
 */
class WatcherOfTheWaysideScenarioTest : ScenarioTestBase() {

    init {
        context("Watcher of the Wayside enters trigger") {

            test("target player mills two cards and the controller gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Watcher of the Wayside")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Watcher of the Wayside")
                withClue("Casting Watcher of the Wayside should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // The enters trigger targets a player; choose Player2 to mill.
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.player2Id))
                }
                game.resolveStack()

                withClue("Watcher of the Wayside should be on the battlefield") {
                    game.isOnBattlefield("Watcher of the Wayside") shouldBe true
                }
                withClue("Target player (Player2) milled exactly two cards") {
                    game.graveyardSize(2) shouldBe 2
                }
                withClue("Controller gains 2 life (20 -> 22)") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }
    }
}
