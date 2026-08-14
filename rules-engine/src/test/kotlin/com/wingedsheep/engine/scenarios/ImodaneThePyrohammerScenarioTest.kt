package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class ImodaneThePyrohammerScenarioTest : ScenarioTestBase() {

    init {
        context("Imodane, the Pyrohammer") {
            test("deals the single-target spell's creature damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Imodane, the Pyrohammer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Lightning Bolt", targetId = creature).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }

            test("triggers only for the sole target and not creatures hit by collateral damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Imodane, the Pyrohammer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Force of Nature", summoningSickness = false)
                    .withCardOnBattlefield(2, "Gurmag Angler", summoningSickness = false)
                    .withCardInHand(1, "Fear, Fire, Foes!")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Force of Nature")!!
                game.castXSpell(1, "Fear, Fire, Foes!", xValue = 2, targetId = target).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
            }

            test("does not trigger when the spell deals damage to a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Imodane, the Pyrohammer", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }

            test("does not trigger for an opponent's spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Imodane, the Pyrohammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Centaur Courser")!!
                game.castSpell(2, "Lightning Bolt", targetId = creature).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
