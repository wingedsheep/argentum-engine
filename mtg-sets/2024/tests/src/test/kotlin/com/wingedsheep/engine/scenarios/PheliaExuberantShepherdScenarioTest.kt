package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Phelia, Exuberant Shepherd (MH3 #40):
 *   "Whenever Phelia attacks, exile up to one other target nonland permanent. At the beginning of
 *    the next end step, return that card to the battlefield under its owner's control. If it
 *    entered under your control, put a +1/+1 counter on Phelia."
 */
class PheliaExuberantShepherdScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.attackWithPhelia() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Phelia, Exuberant Shepherd" to 2)).error shouldBe null
    }

    private fun TestGame.finishCombatAndReachEndStep() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers()
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
    }

    init {
        context("Phelia, Exuberant Shepherd") {

            test("blinking your own permanent returns it and puts a +1/+1 counter on Phelia") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Phelia, Exuberant Shepherd", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithPhelia()
                game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                game.resolveStack()

                withClue("the Bears are exiled until the end step") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }

                game.finishCombatAndReachEndStep()

                val phelia = game.findPermanent("Phelia, Exuberant Shepherd")!!
                withClue("the Bears return under their owner's (your) control") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                withClue("it entered under your control, so Phelia gets a counter") {
                    game.plusCounters(phelia) shouldBe 1
                }
            }

            test("blinking an opponent's permanent returns it to them and gives no counter") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Phelia, Exuberant Shepherd", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithPhelia()
                game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's Bears are exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }

                game.finishCombatAndReachEndStep()

                val phelia = game.findPermanent("Phelia, Exuberant Shepherd")!!
                withClue("the Bears return under their owner's control") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }
                withClue("it didn't enter under your control, so no counter") {
                    game.plusCounters(phelia) shouldBe 0
                }
            }

            test("choosing no target exiles nothing and gives no counter") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Phelia, Exuberant Shepherd", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWithPhelia()
                game.skipTargets()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true

                game.finishCombatAndReachEndStep()

                val phelia = game.findPermanent("Phelia, Exuberant Shepherd")!!
                game.plusCounters(phelia) shouldBe 0
            }
        }
    }
}
