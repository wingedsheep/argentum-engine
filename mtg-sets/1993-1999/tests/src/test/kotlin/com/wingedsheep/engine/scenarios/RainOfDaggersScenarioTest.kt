package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rain of Daggers (P02) — destroys only the target opponent's creatures and
 * costs the caster 2 life per creature actually destroyed.
 */
class RainOfDaggersScenarioTest : ScenarioTestBase() {

    init {
        context("Rain of Daggers") {

            test("destroys opponent's creatures only and caster loses 2 life each") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rain of Daggers")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Rain of Daggers", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 16
                game.getLifeTotal(2) shouldBe 20
            }

            test("no opposing creatures means no life loss") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rain of Daggers")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Rain of Daggers", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
