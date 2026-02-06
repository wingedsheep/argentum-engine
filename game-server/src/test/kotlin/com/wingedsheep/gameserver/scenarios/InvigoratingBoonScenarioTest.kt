package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class InvigoratingBoonScenarioTest : ScenarioTestBase() {

    init {
        context("Invigorating Boon") {
            test("cycling a card triggers Invigorating Boon and puts a +1/+1 counter on target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Invigorating Boon")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature to receive counter
                    .withCardInHand(1, "Barren Moor") // Card with cycling to trigger the boon
                    .withLandsOnBattlefield(1, "Swamp", 1) // To pay cycling cost {B}
                    .withCardInLibrary(1, "Mountain") // Card to draw from cycling
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle Barren Moor - Invigorating Boon triggers
                game.cycleCard(1, "Barren Moor")

                // Boon trigger should require target selection
                withClue("Invigorating Boon should trigger - pending target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker as the target
                val targetId = game.findPermanent("Glory Seeker")
                withClue("Glory Seeker should be on battlefield") {
                    targetId shouldNotBe null
                }
                game.selectTargets(listOf(targetId!!))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // MayEffect should present a yes/no decision
                withClue("Should have may decision after resolving stack") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Glory Seeker should now have a +1/+1 counter
                val counters = game.state.getEntity(targetId)?.get<CountersComponent>()
                withClue("Glory Seeker should have a +1/+1 counter") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("declining the may effect does not add a counter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Invigorating Boon")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Barren Moor")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Barren Moor")

                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                game.resolveStack()

                // Decline the may effect
                game.answerYesNo(false)

                // Glory Seeker should NOT have any counters
                val counters = game.state.getEntity(targetId)?.get<CountersComponent>()
                withClue("Glory Seeker should not have any counters") {
                    val count = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    count shouldBe 0
                }
            }

            test("opponent cycling also triggers Invigorating Boon") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Invigorating Boon")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(2, "Barren Moor") // Opponent has the cycling card
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2) // Opponent is active
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent cycles Barren Moor
                game.cycleCard(2, "Barren Moor")

                // Invigorating Boon should still trigger (controllerOnly = false)
                withClue("Invigorating Boon should trigger from opponent's cycling") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
