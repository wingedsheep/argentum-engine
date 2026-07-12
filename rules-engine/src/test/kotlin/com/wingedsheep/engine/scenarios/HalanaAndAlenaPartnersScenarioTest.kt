package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Halana and Alena, Partners (VOW #239) — {2}{R}{G} Legendary Creature —
 * Human Ranger, 2/3.
 *
 *   First strike
 *   Reach
 *   At the beginning of combat on your turn, put X +1/+1 counters on another target creature you
 *   control, where X is Halana and Alena's power. That creature gains haste until end of turn.
 *
 * Exercises the beginning-of-combat trigger: at 2 power, it puts two +1/+1 counters on another
 * target creature you control and grants that creature haste.
 */
class HalanaAndAlenaPartnersScenarioTest : ScenarioTestBase() {

    init {
        context("Halana and Alena, Partners — begin combat trigger") {

            test("has First Strike and Reach") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Halana and Alena, Partners", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val halanaAndAlena = game.findPermanent("Halana and Alena, Partners")!!
                withClue("has First Strike") {
                    game.state.projectedState.hasKeyword(halanaAndAlena, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("has Reach") {
                    game.state.projectedState.hasKeyword(halanaAndAlena, Keyword.REACH) shouldBe true
                }
            }

            test("puts X +1/+1 counters (X = its power) on another target creature and grants haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Halana and Alena, Partners", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true) // 2/2, has sickness
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val halanaAndAlena = game.findPermanent("Halana and Alena, Partners")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Halana and Alena start at base power 2") {
                    game.state.projectedState.getPower(halanaAndAlena) shouldBe 2
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack() // trigger goes on the stack and asks for a target

                val result = game.selectTargets(listOf(bears))
                withClue("Targeting another creature you control is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears gets 2 +1/+1 counters (X = Halana and Alena's power)") {
                    game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
                withClue("Grizzly Bears becomes a 4/4 with the counters applied") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                withClue("Grizzly Bears gains haste until end of turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
