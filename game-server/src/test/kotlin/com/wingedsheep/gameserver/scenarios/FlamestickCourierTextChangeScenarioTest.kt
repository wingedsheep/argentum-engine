package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for text-changing effects on activated ability target filters and cost filters.
 *
 * Tests two categories:
 * 1. Target filter: Flamestick Courier targets "Goblin creature" -> changed to "Bird creature"
 * 2. Cost filter: Goblin Sledder sacrifices "a Goblin" -> changed to "a Bird"
 */
class FlamestickCourierTextChangeScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldNotBe false
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("text-changed target filter") {

            test("after changing Goblin to Bird, ability can target a Bird creature") {
                val game = scenario()
                    .withPlayers("Red Mage", "Opponent")
                    .withCardOnBattlefield(1, "Flamestick Courier", summoningSickness = false)
                    .withCardOnBattlefield(1, "Sage Aven", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Flamestick Courier: Goblin -> Bird
                val courier = game.findPermanent("Flamestick Courier")!!
                game.castSpell(1, "Artificial Evolution", courier)
                game.resolveStack()
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Bird")

                // Now activate Flamestick Courier's ability targeting Sage Aven (a Bird)
                val cardDef = cardRegistry.getCard("Flamestick Courier")!!
                val ability = cardDef.script.activatedAbilities.first()
                val sageAven = game.findPermanent("Sage Aven")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = courier,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(sageAven))
                    )
                )

                withClue("Should be able to target Sage Aven (Bird) after text change from Goblin to Bird") {
                    result.error.shouldBeNull()
                }
            }

            test("after changing Goblin to Bird, ability can no longer target a Goblin creature") {
                val game = scenario()
                    .withPlayers("Red Mage", "Opponent")
                    .withCardOnBattlefield(1, "Flamestick Courier", summoningSickness = false)
                    .withCardOnBattlefield(1, "Raging Goblin", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Flamestick Courier: Goblin -> Bird
                val courier = game.findPermanent("Flamestick Courier")!!
                game.castSpell(1, "Artificial Evolution", courier)
                game.resolveStack()
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Bird")

                // Try to activate Flamestick Courier's ability targeting Raging Goblin
                val cardDef = cardRegistry.getCard("Flamestick Courier")!!
                val ability = cardDef.script.activatedAbilities.first()
                val ragingGoblin = game.findPermanent("Raging Goblin")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = courier,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(ragingGoblin))
                    )
                )

                withClue("Should NOT be able to target Raging Goblin after text change from Goblin to Bird") {
                    result.error.shouldNotBeNull()
                }
            }
        }

        context("text-changed sacrifice cost filter") {

            test("after changing Goblin to Bird on Goblin Sledder, can sacrifice a Bird") {
                val game = scenario()
                    .withPlayers("Red Mage", "Opponent")
                    .withCardOnBattlefield(1, "Goblin Sledder", summoningSickness = false)
                    .withCardOnBattlefield(1, "Sage Aven", summoningSickness = false)
                    .withCardOnBattlefield(1, "Storm Crow", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Goblin Sledder: Goblin -> Bird
                val sledder = game.findPermanent("Goblin Sledder")!!
                game.castSpell(1, "Artificial Evolution", sledder)
                game.resolveStack()
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Bird")

                // Activate Goblin Sledder's ability: sacrifice Sage Aven (a Bird), target Storm Crow
                val cardDef = cardRegistry.getCard("Goblin Sledder")!!
                val ability = cardDef.script.activatedAbilities.first()
                val sageAven = game.findPermanent("Sage Aven")!!
                val stormCrow = game.findPermanent("Storm Crow")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sledder,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(stormCrow)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(sageAven)
                        )
                    )
                )

                withClue("Should be able to sacrifice Sage Aven (Bird) after text change from Goblin to Bird") {
                    result.error.shouldBeNull()
                }
            }

            test("after changing Goblin to Bird on Goblin Sledder, ability unavailable with only Goblins") {
                // With only Goblins (no Birds) on the battlefield after text change,
                // the sacrifice cost can't be paid at all — no valid permanents to sacrifice
                val game = scenario()
                    .withPlayers("Red Mage", "Opponent")
                    .withCardOnBattlefield(1, "Goblin Sledder", summoningSickness = false)
                    .withCardOnBattlefield(1, "Raging Goblin", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Goblin Sledder: Goblin -> Bird
                val sledder = game.findPermanent("Goblin Sledder")!!
                game.castSpell(1, "Artificial Evolution", sledder)
                game.resolveStack()
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Bird")

                // Try to activate Goblin Sledder's ability: sacrifice Raging Goblin
                // The cost now requires sacrificing a Bird, but Raging Goblin is a Goblin
                val cardDef = cardRegistry.getCard("Goblin Sledder")!!
                val ability = cardDef.script.activatedAbilities.first()
                val ragingGoblin = game.findPermanent("Raging Goblin")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sledder,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(ragingGoblin)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(ragingGoblin)
                        )
                    )
                )

                withClue("Should NOT be able to activate (no Birds to sacrifice) after text change from Goblin to Bird") {
                    result.error.shouldNotBeNull()
                }
            }
        }
    }
}
