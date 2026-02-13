package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Skirk Fire Marshal.
 *
 * Card reference:
 * - Skirk Fire Marshal ({3}{R}{R}): 2/2 Creature — Goblin
 *   "Protection from red
 *    Tap five untapped Goblins you control: Skirk Fire Marshal deals 10 damage
 *    to each creature and each player."
 */
class SkirkFireMarshalScenarioTest : ScenarioTestBase() {

    init {
        context("Skirk Fire Marshal tap five goblins ability") {

            test("deals 10 damage to each creature and each player") {
                val game = scenario()
                    .withPlayers("GoblinLord", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Fire Marshal")
                    .withCardOnBattlefield(1, "Goblin Sledder")
                    .withCardOnBattlefield(1, "Goblin Sledder")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardOnBattlefield(1, "Goblin Sky Raider")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3, should die to 10 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val marshal = game.findPermanent("Skirk Fire Marshal")!!
                val goblins = listOf(marshal) +
                    game.findAllPermanents("Goblin Sledder") +
                    game.findAllPermanents("Festering Goblin") +
                    game.findAllPermanents("Goblin Sky Raider")

                withClue("Should have 5 goblins") {
                    goblins.size shouldBe 5
                }

                val cardDef = cardRegistry.getCard("Skirk Fire Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshal,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = goblins)
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Hill Giant (3/3) should be destroyed by 10 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }

                withClue("Non-protected goblins should be destroyed by 10 damage") {
                    game.isOnBattlefield("Goblin Sledder") shouldBe false
                    game.isOnBattlefield("Festering Goblin") shouldBe false
                    game.isOnBattlefield("Goblin Sky Raider") shouldBe false
                }

                withClue("Skirk Fire Marshal should survive due to protection from red") {
                    game.isOnBattlefield("Skirk Fire Marshal") shouldBe true
                }

                withClue("Both players should take 10 damage (20 -> 10)") {
                    game.getLifeTotal(1) shouldBe 10
                    game.getLifeTotal(2) shouldBe 10
                }
            }

            test("cannot activate with fewer than five goblins") {
                val game = scenario()
                    .withPlayers("GoblinLord", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Fire Marshal")
                    .withCardOnBattlefield(1, "Goblin Sledder")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    // Only 3 goblins total
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val marshal = game.findPermanent("Skirk Fire Marshal")!!
                val goblins = listOf(marshal) +
                    game.findAllPermanents("Goblin Sledder") +
                    game.findAllPermanents("Festering Goblin")

                withClue("Should have only 3 goblins") {
                    goblins.size shouldBe 3
                }

                val cardDef = cardRegistry.getCard("Skirk Fire Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshal,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = goblins)
                    )
                )

                withClue("Ability should fail with insufficient goblins") {
                    result.error shouldNotBe null
                }
            }

            test("protection from red prevents the damage to the marshal") {
                val game = scenario()
                    .withPlayers("GoblinLord", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Fire Marshal")
                    .withCardOnBattlefield(1, "Skirk Prospector") // Goblin
                    .withCardOnBattlefield(1, "Goblin Sledder")
                    .withCardOnBattlefield(1, "Goblin Taskmaster")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val marshal = game.findPermanent("Skirk Fire Marshal")!!
                val goblins = listOf(marshal) +
                    game.findAllPermanents("Skirk Prospector") +
                    game.findAllPermanents("Goblin Sledder") +
                    game.findAllPermanents("Goblin Taskmaster") +
                    game.findAllPermanents("Festering Goblin")

                val cardDef = cardRegistry.getCard("Skirk Fire Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshal,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = goblins)
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Skirk Fire Marshal should survive - protection from red prevents the damage") {
                    game.isOnBattlefield("Skirk Fire Marshal") shouldBe true
                }
            }
        }
    }
}
