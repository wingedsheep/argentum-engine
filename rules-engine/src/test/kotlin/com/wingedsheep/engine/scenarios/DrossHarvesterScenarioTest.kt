package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dross Harvester (MRD) — protection from white; "At the beginning of your end step, you lose 4
 * life"; "Whenever a creature dies, you gain 2 life."
 *
 * The death trigger is deliberately battlefield-wide ([com.wingedsheep.sdk.dsl.Triggers.AnyCreatureDies]):
 * *any* creature, either player's, and the Harvester itself. That last case is the one a self- or
 * you-control-scoped trigger would silently drop, so it gets its own test.
 */
class DrossHarvesterScenarioTest : ScenarioTestBase() {

    init {
        context("Dross Harvester") {
            test("your end step drains you for 4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dross Harvester")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the upkeep-style drain is a straight 4 off the controller") {
                    game.getLifeTotal(1) shouldBe 16
                }
            }

            test("an opponent's creature dying gains you 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dross Harvester")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Stonesplitter Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castXSpell(1, "Stonesplitter Bolt", xValue = 2, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the trigger is 'a creature', not 'a creature you control'") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("the Harvester's own death still gains you 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dross Harvester")
                    .withCardInHand(1, "Stonesplitter Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val harvester = game.findPermanent("Dross Harvester").shouldNotBeNull()

                game.castXSpell(1, "Stonesplitter Bolt", xValue = 4, targetId = harvester).error shouldBe null
                game.resolveStack()

                withClue("its own death is a creature dying — the trigger fires off last-known info") {
                    game.isOnBattlefield("Dross Harvester") shouldBe false
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }
    }
}
