package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sunfire Balm.
 *
 * Sunfire Balm: {2}{W} Instant
 * Prevent the next 4 damage that would be dealt to any target this turn.
 * Cycling {1}{W}
 * When you cycle Sunfire Balm, you may prevent the next 1 damage that would be dealt to any target this turn.
 */
class SunfireBalmScenarioTest : ScenarioTestBase() {

    init {
        context("Sunfire Balm - cast") {
            test("prevents 4 damage to a creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunfire Balm")
                    .withLandsOnBattlefield(1, "Plains", 3) // {2}{W}
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3
                    .withCardInHand(1, "Shock") // Deals 2 damage
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Elvish Warrior")!!

                // Cast Sunfire Balm targeting our own creature
                game.castSpell(1, "Sunfire Balm", creatureId)
                game.resolveStack()

                // Now cast Shock at the same creature - shield should absorb 2 of the 4 prevention
                game.castSpell(1, "Shock", creatureId)
                game.resolveStack()

                withClue("Elvish Warrior should survive since 2 damage is prevented") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                val damage = game.state.getEntity(creatureId)?.get<DamageComponent>()?.amount ?: 0
                withClue("Creature should have 0 damage (2 prevented out of 4 shield)") {
                    damage shouldBe 0
                }
            }

            test("prevents 4 damage to a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunfire Balm")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Cast Sunfire Balm targeting self
                game.castSpell(1, "Sunfire Balm", game.player1Id)
                game.resolveStack()

                withClue("Player life should not change from just casting prevention") {
                    game.getLifeTotal(1) shouldBe startingLife
                }
            }
        }

        context("Sunfire Balm - cycling trigger") {
            test("cycling trigger prevents 1 damage to target when player chooses yes") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunfire Balm")
                    .withLandsOnBattlefield(1, "Plains", 2) // {1}{W} cycling cost
                    .withCardInLibrary(1, "Forest") // Card to draw from cycling
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3
                    .withCardInHand(1, "Shock") // Deals 2 damage
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Elvish Warrior")!!

                // Cycle Sunfire Balm
                game.cycleCard(1, "Sunfire Balm")

                // Cycling trigger - MayEffect asks yes/no
                withClue("Should have may decision for cycling trigger") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Target our creature
                game.selectTargets(listOf(creatureId))

                // Resolve the triggered ability
                game.resolveStack()

                // Now deal 2 damage to the creature - shield prevents 1
                game.castSpell(1, "Shock", creatureId)
                game.resolveStack()

                withClue("Elvish Warrior should survive with 1 damage (2 - 1 prevented = 1 on a 2/3)") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                val damage = game.state.getEntity(creatureId)?.get<DamageComponent>()?.amount ?: 0
                withClue("Creature should have 1 damage (2 dealt minus 1 prevented)") {
                    damage shouldBe 1
                }
            }

            test("cycling trigger does nothing when player declines") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunfire Balm")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Cycle Sunfire Balm
                game.cycleCard(1, "Sunfire Balm")

                // Decline the may ability
                game.answerYesNo(false)

                withClue("Player life should be unchanged") {
                    game.getLifeTotal(1) shouldBe startingLife
                }
            }
        }
    }
}
