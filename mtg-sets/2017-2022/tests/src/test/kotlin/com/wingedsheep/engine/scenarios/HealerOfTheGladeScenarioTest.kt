package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Healer of the Glade (M20 #176) — {G} Creature — Elemental 1/2.
 * "When this creature enters, you gain 3 life."
 */
class HealerOfTheGladeScenarioTest : ScenarioTestBase() {
    init {
        context("Healer of the Glade") {
            test("entering the battlefield gains its controller 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Healer of the Glade")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Healer of the Glade").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Healer of the Glade") shouldBe true
                withClue("ETB trigger gains 3 life (20 -> 23); the opponent is untouched") {
                    game.getLifeTotal(1) shouldBe 23
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
