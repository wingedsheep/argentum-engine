package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gaea's Liege — a CDA whose count switches from "Forests you control" to
 * "Forests defending player controls" while it attacks, plus a land-type change that lasts only
 * while the Liege stays on the battlefield.
 */
class GaeasLiegeScenarioTest : ScenarioTestBase() {

    init {
        context("Gaea's Liege") {

            test("not attacking, it counts the Forests you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gaea's Liege")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liege = game.findPermanent("Gaea's Liege")!!
                game.state.projectedState.getPower(liege) shouldBe 4
                game.state.projectedState.getToughness(liege) shouldBe 4
            }

            test("while attacking, it counts the Forests the defending player controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gaea's Liege", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liege = game.findPermanent("Gaea's Liege")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Gaea's Liege" to 2)).error shouldBe null

                withClue("defending player controls 2 Forests") {
                    game.state.projectedState.getPower(liege) shouldBe 2
                    game.state.projectedState.getToughness(liege) shouldBe 2
                }
            }

            test("the land becomes a Forest, feeds the count, and reverts when the Liege leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gaea's Liege", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(1, "Terror")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liege = game.findPermanent("Gaea's Liege")!!
                val converted = game.findPermanents("Swamp").first()
                game.state.projectedState.getPower(liege) shouldBe 3

                val ability = cardRegistry.getCard("Gaea's Liege")!!.script.activatedAbilities[0]
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = liege,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(converted))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the Swamp is now a Forest and nothing else") {
                    game.state.projectedState.hasSubtype(converted, "Forest") shouldBe true
                    game.state.projectedState.hasSubtype(converted, "Swamp") shouldBe false
                }
                withClue("the converted land counts toward the Liege") {
                    game.state.projectedState.getPower(liege) shouldBe 4
                    game.state.projectedState.getToughness(liege) shouldBe 4
                }

                game.castSpell(1, "Terror", liege).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Gaea's Liege") shouldBe false

                withClue("the effect ends when the Liege leaves the battlefield") {
                    game.state.projectedState.hasSubtype(converted, "Swamp") shouldBe true
                    game.state.projectedState.hasSubtype(converted, "Forest") shouldBe false
                }
            }
        }
    }
}
