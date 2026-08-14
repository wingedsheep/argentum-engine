package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Shrewd Storyteller. */
class ShrewdStorytellerScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Shrewd Storyteller — Survival trigger") {
            test("a tapped Storyteller puts a +1/+1 counter on a target creature at second main") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shrewd Storyteller", tapped = true)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Centaur Courser")!!

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack(); guard++
                }
                val td = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected ChooseTargetsDecision for Survival trigger; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bears))))
                game.resolveStack()

                withClue("Grizzly Bears gains a +1/+1 counter") {
                    val counters = game.state.getEntity(bears)?.get<CountersComponent>()?.counters ?: emptyMap()
                    counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
                }
            }

            test("an untapped Storyteller does NOT fire the Survival trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shrewd Storyteller", tapped = false)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Centaur Courser")!!
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { if (game.hasPendingDecision()) Unit else game.resolveStack() }

                withClue("No Survival trigger — the Storyteller is untapped") {
                    (game.state.pendingDecision is ChooseTargetsDecision) shouldBe false
                    val counters = game.state.getEntity(bears)?.get<CountersComponent>()?.counters ?: emptyMap()
                    counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe null
                }
            }
        }
    }
}
