package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Snarling Wolf (MID #199, reprinted VOW #219).
 *
 * {G} Creature — Wolf, 1/1
 * "{1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."
 */
class SnarlingWolfScenarioTest : ScenarioTestBase() {

    init {
        context("Snarling Wolf") {

            test("activating {1}{G} gives it +2/+2 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Snarling Wolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Snarling Wolf")!!
                val abilityId = cardRegistry.getCard("Snarling Wolf")!!
                    .script.activatedAbilities[0].id

                game.state.projectedState.getPower(wolf) shouldBe 1
                game.state.projectedState.getToughness(wolf) shouldBe 1

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wolf,
                        abilityId = abilityId
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Snarling Wolf should be 3/3 after +2/+2") {
                    game.state.projectedState.getPower(wolf) shouldBe 3
                    game.state.projectedState.getToughness(wolf) shouldBe 3
                }
            }

            test("the pump ability can only be activated once each turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Snarling Wolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Snarling Wolf")!!
                val abilityId = cardRegistry.getCard("Snarling Wolf")!!
                    .script.activatedAbilities[0].id

                withClue("the pump ability should be available before any activation") {
                    game.getLegalActions(1).find {
                        it.actionType == "ActivateAbility" &&
                            (it.action as? ActivateAbility)?.sourceId == wolf
                    } shouldNotBe null
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wolf,
                        abilityId = abilityId
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the pump ability should NOT be available a second time this turn") {
                    game.getLegalActions(1).find {
                        it.actionType == "ActivateAbility" &&
                            (it.action as? ActivateAbility)?.sourceId == wolf
                    } shouldBe null
                }
            }
        }
    }
}
