package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Kessig Flamebreather (VOW #164) — {1}{R} Creature — Human Shaman, 1/3.
 *
 *   Whenever you cast a noncreature spell, this creature deals 1 damage to each opponent.
 *
 * Exercises the noncreature-cast trigger: casting an instant (Lightning Bolt) triggers Kessig
 * Flamebreather's 1 damage to each opponent, on top of the Bolt's own 3 damage. Also verifies the
 * trigger does NOT fire off a creature spell.
 */
class KessigFlamebreatherScenarioTest : ScenarioTestBase() {

    init {
        context("Kessig Flamebreather — noncreature-cast damage trigger") {

            test("casting a noncreature spell deals 1 damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kessig Flamebreather", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Kessig Flamebreather's 1 + Lightning Bolt's 3 = 4 damage (20 -> 16)") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("casting a creature spell does not trigger the damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kessig Flamebreather", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("a creature spell does not trigger Kessig Flamebreather") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
