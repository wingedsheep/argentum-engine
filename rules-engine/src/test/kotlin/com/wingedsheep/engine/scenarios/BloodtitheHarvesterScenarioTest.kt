package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Bloodtithe Harvester (VOW #232) — {B}{R} Creature — Vampire, 3/2.
 *
 *   When this creature enters, create a Blood token.
 *   {T}, Sacrifice this creature: Target creature gets -X/-X until end of turn, where X is twice
 *   the number of Blood tokens you control. Activate only as a sorcery.
 *
 * Exercises the ETB Blood token creation and the tap-sacrifice ability's -X/-X sizing: with two
 * Blood tokens under the controller, X = 4, so the target gets -4/-4.
 */
class BloodtitheHarvesterScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodtithe Harvester") {

            test("entering the battlefield creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bloodtithe Harvester")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Bloodtithe Harvester").error shouldBe null
                game.resolveStack()

                withClue("a Blood token is created on entering the battlefield") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }

            test("tap+sacrifice gives target -X/-X where X is twice the controller's Blood count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodtithe Harvester", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardOnBattlefield(2, "Force of Nature", summoningSickness = false) // 5/5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val harvester = game.findPermanent("Bloodtithe Harvester")!!
                val target = game.findPermanent("Force of Nature")!!
                val abilityId = cardRegistry.getCard("Bloodtithe Harvester")!!.activatedAbilities.first().id

                withClue("two Blood tokens are on the battlefield before activation") {
                    game.findPermanents("Blood").size shouldBe 2
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = harvester,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(target))
                    )
                )
                withClue("activation should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Bloodtithe Harvester was sacrificed to pay the cost") {
                    game.isOnBattlefield("Bloodtithe Harvester") shouldBe false
                    game.isInGraveyard(1, "Bloodtithe Harvester") shouldBe true
                }
                withClue("X = 2 * 2 Blood tokens = 4, so Force of Nature (5/5) becomes 1/1") {
                    game.state.projectedState.getPower(target) shouldBe 1
                    game.state.projectedState.getToughness(target) shouldBe 1
                }
            }
        }
    }
}
