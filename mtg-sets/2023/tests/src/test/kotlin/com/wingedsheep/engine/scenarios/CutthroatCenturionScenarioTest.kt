package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cutthroat Centurion (ONE #89) — {2}{B} Artifact Creature — Phyrexian Warrior 2/2.
 * "Sacrifice another artifact or creature: This creature gets +2/+2 until end of turn.
 * Activate only once each turn."
 */
class CutthroatCenturionScenarioTest : ScenarioTestBase() {

    init {
        context("Cutthroat Centurion") {

            test("sacrificing another artifact pumps it +2/+2, and only once per turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cutthroat Centurion", summoningSickness = false)
                    .withCardOnBattlefield(1, "Millstone")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val centurionId = game.findPermanent("Cutthroat Centurion")!!
                val artifactId = game.findPermanent("Millstone")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Cutthroat Centurion")!!.script.activatedAbilities[0]

                game.state.projectedState.getPower(centurionId) shouldBe 2

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = centurionId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifactId))
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Millstone") shouldBe false
                game.state.projectedState.getPower(centurionId) shouldBe 4
                game.state.projectedState.getToughness(centurionId) shouldBe 4

                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = centurionId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bearsId))
                    )
                )
                withClue("A second activation in the same turn must be rejected") {
                    second.error shouldNotBe null
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("cannot sacrifice itself to its own ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cutthroat Centurion", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val centurionId = game.findPermanent("Cutthroat Centurion")!!
                val ability = cardRegistry.getCard("Cutthroat Centurion")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = centurionId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(centurionId))
                    )
                )
                result.error shouldNotBe null
                game.isOnBattlefield("Cutthroat Centurion") shouldBe true
            }
        }
    }
}
