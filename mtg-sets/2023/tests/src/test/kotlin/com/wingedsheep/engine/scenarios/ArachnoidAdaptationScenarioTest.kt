package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Arachnoid Adaptation. */
class ArachnoidAdaptationScenarioTest : ScenarioTestBase() {

    init {
        context("Arachnoid Adaptation — +2/+2, reach, untap") {
            test("pumps a tapped creature, grants reach, and untaps it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Arachnoid Adaptation")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

                game.castSpell(1, "Arachnoid Adaptation", bears)
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("gets +2/+2") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                }
                withClue("gains reach") {
                    projected.hasKeyword(bears, Keyword.REACH) shouldBe true
                }
                withClue("untapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
