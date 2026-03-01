package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Trap Digger.
 *
 * Trap Digger: {3}{W} 1/3 Creature — Human Soldier
 * {2}{W}, {T}: Put a trap counter on target land you control.
 * Sacrifice a land with a trap counter on it: Trap Digger deals 3 damage
 * to target attacking creature without flying.
 */
class TrapDiggerScenarioTest : ScenarioTestBase() {

    init {
        context("Trap Digger first ability - put trap counter on land") {

            test("puts a trap counter on target land you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Trap Digger")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains") // 4 lands for {2}{W} + one to target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val diggerId = game.findPermanent("Trap Digger")!!
                val landId = game.findAllPermanents("Plains").first()

                val cardDef = cardRegistry.getCard("Trap Digger")!!
                // First ability: {2}{W}, {T}: Put a trap counter on target land you control.
                val ability = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = diggerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(landId))
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Land should now have a trap counter
                val landContainer = game.state.getEntity(landId)!!
                val counters = landContainer.get<CountersComponent>()
                withClue("Land should have a trap counter") {
                    counters shouldBe CountersComponent(mapOf(CounterType.TRAP to 1))
                }
            }
        }

        context("Trap Digger second ability - sacrifice land with trap counter") {

            test("deals 3 damage to attacking creature without flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Trap Digger")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 attacker
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val diggerId = game.findPermanent("Trap Digger")!!
                val landId = game.findAllPermanents("Plains").first()
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Manually put a trap counter on the land
                val countersComp = CountersComponent(mapOf(CounterType.TRAP to 1))
                game.state = game.state.updateEntity(landId) { it.with(countersComp) }

                // Declare Grizzly Bears as attacker
                game.declareAttackers(mapOf("Grizzly Bears" to 1))

                // Active player (P2) passes priority so defending player (P1) gets it
                game.passPriority()

                val cardDef = cardRegistry.getCard("Trap Digger")!!
                // Second ability: Sacrifice a land with a trap counter: deal 3 damage
                val ability = cardDef.script.activatedAbilities[1]

                val costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(landId)
                )

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = diggerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId)),
                        costPayment = costPayment
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Grizzly Bears (2/2) took 3 damage, should be dead
                withClue("Grizzly Bears should be destroyed by 3 damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                // Land should be sacrificed
                withClue("Land should be sacrificed") {
                    game.findAllPermanents("Plains").size shouldBe 0
                }
            }
        }

        context("HasCounter state predicate filtering") {

            test("cannot sacrifice a land without a trap counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Trap Digger")
                    .withCardOnBattlefield(1, "Plains") // No trap counter
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val landId = game.findAllPermanents("Plains").first()
                val diggerId = game.findPermanent("Trap Digger")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Grizzly Bears as attacker
                game.declareAttackers(mapOf("Grizzly Bears" to 1))

                // Active player (P2) passes priority so defending player (P1) gets it
                game.passPriority()

                val cardDef = cardRegistry.getCard("Trap Digger")!!
                val ability = cardDef.script.activatedAbilities[1]

                val costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(landId)
                )

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = diggerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId)),
                        costPayment = costPayment
                    )
                )

                withClue("Activation should fail because land has no trap counter") {
                    result.error shouldBe "Cannot pay ability cost"
                }
            }
        }
    }
}
