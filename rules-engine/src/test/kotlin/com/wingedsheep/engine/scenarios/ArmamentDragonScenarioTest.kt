package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Armament Dragon. */
class ArmamentDragonScenarioTest : ScenarioTestBase() {

    private val agentRenewAbilityId =
        cardRegistry.getCard("Agent of Kotis")!!.activatedAbilities.first().id

    init {
        context("Armament Dragon") {
            test("flying, and ETB distributes three +1/+1 counters among target creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Armament Dragon")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears") // distribute target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!

                // Cast the Dragon; resolving the stack puts it on the battlefield and fires its ETB.
                withClue("Casting Armament Dragon should succeed") {
                    game.castSpell(1, "Armament Dragon").error shouldBe null
                }
                game.resolveStack() // dragon enters → ETB trigger asks for targets

                val dragon = game.findPermanent("Armament Dragon")!!
                withClue("Armament Dragon has flying") {
                    game.state.projectedState.hasKeyword(dragon, Keyword.FLYING) shouldBe true
                }

                // Choose the Bear as the single target, then distribute all three counters onto it.
                withClue("Targeting the Bear should be legal") {
                    game.selectTargets(listOf(bear)).error shouldBe null
                }
                game.resolveStack()

                if (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision() as DistributeDecision
                    game.submitDecision(
                        DistributionResponse(decision.id, mapOf(bear to decision.totalAmount))
                    )
                    game.resolveStack()
                }

                withClue("All three +1/+1 counters land on the single target") {
                    game.state.getEntity(bear)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
            }
        }
    }
}
