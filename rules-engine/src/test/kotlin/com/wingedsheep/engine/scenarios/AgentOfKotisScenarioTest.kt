package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Agent of Kotis. */
class AgentOfKotisScenarioTest : ScenarioTestBase() {

    private val agentRenewAbilityId =
        cardRegistry.getCard("Agent of Kotis")!!.activatedAbilities.first().id

    init {
        context("Agent of Kotis") {
            test("renew puts two +1/+1 counters on target creature, exiling Agent from the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Agent of Kotis")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Island", 4) // renew cost {3}{U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val agent = game.findCardsInGraveyard(1, "Agent of Kotis").first()
                val bear = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = agent,
                        abilityId = agentRenewAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bear)),
                    )
                )
                withClue("Activating Agent of Kotis renew should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears gets two +1/+1 counters") {
                    game.state.getEntity(bear)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
                withClue("Agent of Kotis is exiled from the graveyard as part of the cost") {
                    game.findCardsInGraveyard(1, "Agent of Kotis").size shouldBe 0
                    game.state.getExile(game.player1Id).contains(agent) shouldBe true
                }
            }
        }
    }
}
