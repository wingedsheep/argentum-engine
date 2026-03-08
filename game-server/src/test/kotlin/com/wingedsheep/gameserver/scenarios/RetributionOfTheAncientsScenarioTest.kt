package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Retribution of the Ancients.
 *
 * Card reference:
 * - Retribution of the Ancients ({B}): Enchantment
 *   {B}, Remove X +1/+1 counters from among creatures you control:
 *   Target creature gets -X/-X until end of turn.
 */
class RetributionOfTheAncientsScenarioTest : ScenarioTestBase() {

    private fun TestGame.getCounters(name: String): Int {
        val entityId = findPermanent(name) ?: return 0
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun TestGame.addPlusOnePlusOneCounters(name: String, count: Int) {
        val entityId = findPermanent(name)!!
        state = state.updateEntity(entityId) { container ->
            val counters = container.get<CountersComponent>() ?: CountersComponent()
            container.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
    }

    init {
        context("Retribution of the Ancients activated ability") {

            test("removes counters and gives -X/-X to target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retribution of the Ancients")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Add 3 +1/+1 counters to Glory Seeker
                game.addPlusOnePlusOneCounters("Glory Seeker", 3)

                val retributionId = game.findPermanent("Retribution of the Ancients")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!

                val cardDef = cardRegistry.getCard("Retribution of the Ancients")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate with X=2 to give Hill Giant -2/-2
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = retributionId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId)),
                        xValue = 2
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Glory Seeker should have 1 +1/+1 counter remaining (3 - 2 = 1)
                game.getCounters("Glory Seeker") shouldBe 1

                // Resolve the ability
                game.resolveStack()

                // Hill Giant (3/3) should get -2/-2, becoming 1/1
                val clientState = game.getClientState(2)
                val hillGiantInfo = clientState.cards[hillGiantId]
                withClue("Hill Giant should be 1/1 after -2/-2") {
                    hillGiantInfo.shouldNotBeNull()
                    hillGiantInfo.power shouldBe 1
                    hillGiantInfo.toughness shouldBe 1
                }
            }

            test("X=3 kills a 3/3 creature by giving -3/-3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retribution of the Ancients")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Add 3 +1/+1 counters to Glory Seeker
                game.addPlusOnePlusOneCounters("Glory Seeker", 3)

                val retributionId = game.findPermanent("Retribution of the Ancients")!!
                val hillGiantId = game.findPermanent("Hill Giant")!!

                val cardDef = cardRegistry.getCard("Retribution of the Ancients")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate with X=3 to kill Hill Giant
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = retributionId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId)),
                        xValue = 3
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Glory Seeker should have 0 +1/+1 counters remaining
                game.getCounters("Glory Seeker") shouldBe 0

                // Resolve the ability
                game.resolveStack()

                // Hill Giant should be dead (3/3 with -3/-3 = 0/0)
                withClue("Hill Giant should be dead") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }

            test("distributes counter removal across multiple creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Retribution of the Ancients")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 on P1's side too
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Add 2 counters to Glory Seeker and 1 to Hill Giant
                game.addPlusOnePlusOneCounters("Glory Seeker", 2)
                game.addPlusOnePlusOneCounters("Hill Giant", 1)

                val retributionId = game.findPermanent("Retribution of the Ancients")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Retribution of the Ancients")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate with X=3 (total available across both creatures)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = retributionId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId)),
                        xValue = 3
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Total counters removed should be 3 (distributed across both creatures)
                val totalRemaining = game.getCounters("Glory Seeker") + game.getCounters("Hill Giant")
                totalRemaining shouldBe 0

                // Resolve the ability
                game.resolveStack()

                // Grizzly Bears (2/2) with -3/-3 should be dead
                withClue("Grizzly Bears should be dead") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
