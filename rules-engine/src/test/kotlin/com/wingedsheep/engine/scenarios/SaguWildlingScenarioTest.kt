package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Sagu Wildling. */
class SaguWildlingScenarioTest : ScenarioTestBase() {

    init {
        context("Sagu Wildling") {
            test("ETB gains the controller 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sagu Wildling")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Sagu Wildling")
                withClue("Casting Sagu Wildling should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Sagu Wildling should be on the battlefield") {
                    game.isOnBattlefield("Sagu Wildling") shouldBe true
                }
                withClue("Controller should have gained 3 life (20 -> 23)") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }
        }
    }
}
