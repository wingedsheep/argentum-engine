package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Grasp of the Hieromancer (Magic Origins).
 *
 * Oracle: "Enchant creature / Enchanted creature gets +1/+1 and has 'Whenever this creature
 * attacks, tap target creature defending player controls.'"
 *
 * Under test: the granted attack trigger resolves "defending player" off the enchanted attacker
 * while its target is being chosen, and taps the defender's creature before blockers.
 */
class GraspOfTheHieromancerScenarioTest : ScenarioTestBase() {

    init {
        context("Grasp of the Hieromancer — enchanted creature attacks") {

            test("gets +1/+1 and taps a creature the defending player controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Grasp of the Hieromancer", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                withClue("enchanted creature gets +1/+1") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("the granted attack trigger must announce a target") {
                    game.state.pendingDecision shouldNotBe null
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the defending player's creature is tapped before blockers") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
