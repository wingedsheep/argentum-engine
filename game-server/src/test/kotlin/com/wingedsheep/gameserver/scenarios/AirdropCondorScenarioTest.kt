package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Airdrop Condor.
 *
 * Card reference:
 * - Airdrop Condor ({4}{R}): Creature — Bird 2/2
 *   Flying
 *   {1}{R}, Sacrifice a Goblin creature: Airdrop Condor deals damage equal to
 *   the sacrificed creature's power to any target.
 */
class AirdropCondorScenarioTest : ScenarioTestBase() {

    init {
        context("Airdrop Condor activated ability") {
            test("deals damage equal to sacrificed Goblin's power to target player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Airdrop Condor")
                    .withCardOnBattlefield(1, "Skirk Commando") // 2/1 Goblin
                    .withLandsOnBattlefield(1, "Mountain", 2) // {1}{R} for activation
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val condorId = game.findPermanent("Airdrop Condor")!!
                val goblinId = game.findPermanent("Skirk Commando")!!

                val cardDef = cardRegistry.getCard("Airdrop Condor")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate ability: sacrifice Skirk Commando, target player 2
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = condorId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(goblinId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Skirk Commando should be sacrificed
                withClue("Skirk Commando should be in graveyard") {
                    game.isOnBattlefield("Skirk Commando") shouldBe false
                    game.isInGraveyard(1, "Skirk Commando") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Player 2 should take 2 damage (Skirk Commando's power)
                withClue("Player 2 should take 2 damage from sacrificed Skirk Commando (power 2)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("deals damage equal to sacrificed Goblin's power to target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Airdrop Condor")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // 1/2 Goblin
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val condorId = game.findPermanent("Airdrop Condor")!!
                val goblinId = game.findPermanent("Goblin Sky Raider")!!
                val seekerId = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Airdrop Condor")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate ability: sacrifice Goblin Sky Raider (power 1), target Glory Seeker
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = condorId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(seekerId)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(goblinId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Glory Seeker (2/2) should survive 1 damage from Goblin Sky Raider (power 1)
                withClue("Glory Seeker should survive 1 damage (2/2 with 1 damage)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }
        }
    }
}
