package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Stern Marshal's activated ability.
 *
 * Stern Marshal: {2}{W} 2/2 Creature - Human Soldier
 * "{T}: Target creature gets +2/+2 until end of turn. Activate only during your turn,
 * before attackers are declared."
 *
 * These tests verify:
 * 1. The ability can be activated during precombat main phase (before attackers)
 * 2. The ability cannot be activated after declare attackers step
 * 3. The ability cannot be activated during opponent's turn
 * 4. The ability correctly gives +2/+2 to a target creature
 */
class SternMarshalScenarioTest : ScenarioTestBase() {

    init {
        context("Stern Marshal activated ability") {

            test("can activate during precombat main phase to give +2/+2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stern Marshal")  // 2/2
                    .withCardOnBattlefield(1, "Devoted Hero")   // 1/1 target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val marshalId = game.findPermanent("Stern Marshal")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Verify Devoted Hero starts at 1/2 (base stats)
                val initialClientState = game.getClientState(1)
                val initialHeroInfo = initialClientState.cards[heroId]
                withClue("Devoted Hero should start as 1/2") {
                    initialHeroInfo shouldNotBe null
                    initialHeroInfo!!.power shouldBe 1
                    initialHeroInfo.toughness shouldBe 2
                }

                // Find Stern Marshal's activated ability
                val cardDef = cardRegistry.getCard("Stern Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability targeting Devoted Hero
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshalId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Stern Marshal should be tapped after activation
                val marshalContainer = game.state.getEntity(marshalId)!!
                withClue("Stern Marshal should be tapped after activating tap ability") {
                    marshalContainer.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                }

                // Resolve the ability by both players passing priority
                game.resolveStack()

                // Verify Devoted Hero is now 3/4 (+2/+2 from base 1/2)
                val finalClientState = game.getClientState(1)
                val finalHeroInfo = finalClientState.cards[heroId]
                withClue("Devoted Hero should be 3/4 after Stern Marshal's ability resolves") {
                    finalHeroInfo shouldNotBe null
                    finalHeroInfo!!.power shouldBe 3
                    finalHeroInfo.toughness shouldBe 4
                }
            }

            test("cannot activate during declare attackers step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stern Marshal")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val marshalId = game.findPermanent("Stern Marshal")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("Stern Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to activate - should fail because we're at/past declare attackers
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshalId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should fail at declare attackers step") {
                    result.error shouldNotBe null
                }
            }

            test("cannot activate during opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stern Marshal")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val marshalId = game.findPermanent("Stern Marshal")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("Stern Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to activate during opponent's turn - should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshalId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should fail during opponent's turn") {
                    result.error shouldNotBe null
                }
            }

            test("can activate during begin combat step (before attackers)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stern Marshal")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val marshalId = game.findPermanent("Stern Marshal")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("Stern Marshal")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = marshalId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should succeed during begin combat: ${result.error}") {
                    result.error shouldBe null
                }
            }
        }
    }
}
