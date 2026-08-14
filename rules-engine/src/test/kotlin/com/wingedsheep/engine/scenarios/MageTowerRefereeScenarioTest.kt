package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Mage Tower Referee. */
class MageTowerRefereeScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int {
        val id = findPermanent(name) ?: return 0
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Mage Tower Referee — multicolored cast trigger") {
            test("gets a +1/+1 counter when you cast a multicolored spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mage Tower Referee", summoningSickness = false)
                    .withCardInHand(1, "Llanowar Knight") // {G}{W} multicolored creature
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("starts with no +1/+1 counters") { game.plusCounters("Mage Tower Referee") shouldBe 0 }

                game.castSpell(1, "Llanowar Knight").error shouldBe null
                game.resolveStack()

                withClue("casting a multicolored spell adds one +1/+1 counter") {
                    game.plusCounters("Mage Tower Referee") shouldBe 1
                }
            }

            test("does NOT trigger on a monocolored spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mage Tower Referee", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears") // {1}{G} monocolored
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("a monocolored spell does not add a counter") {
                    game.plusCounters("Mage Tower Referee") shouldBe 0
                }
            }
        }
    }
}
