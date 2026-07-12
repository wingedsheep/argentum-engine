package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Flourishing Hunter (VOW #199) — {4}{G}{G} Creature — Wolf Spirit, 6/6.
 *
 *   When this creature enters, you gain life equal to the greatest toughness among other
 *   creatures you control.
 *
 * Exercises the MAX-toughness aggregate with excludeSelf=true: with a 2/2 and a 3/3 other creature
 * you control, life gained is 3 (the greatest OTHER toughness), not Flourishing Hunter's own 6.
 */
class FlourishingHunterScenarioTest : ScenarioTestBase() {

    init {
        context("Flourishing Hunter ETB") {

            test("gains life equal to the greatest toughness among other creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flourishing Hunter")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flourishing Hunter").error shouldBe null
                game.resolveStack()

                withClue("Flourishing Hunter entered the battlefield") {
                    game.isOnBattlefield("Flourishing Hunter") shouldBe true
                }
                withClue("gained 3 life (Hill Giant's toughness, the greatest among other creatures)") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }

            test("with no other creatures, gains no life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flourishing Hunter")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flourishing Hunter").error shouldBe null
                game.resolveStack()

                withClue("no other creatures you control -> no life gained") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
