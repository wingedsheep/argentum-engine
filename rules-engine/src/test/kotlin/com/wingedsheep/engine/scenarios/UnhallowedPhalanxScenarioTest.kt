package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Unhallowed Phalanx (VOW #135) — {4}{B} Creature — Zombie Soldier, 1/13.
 *
 *   This creature enters tapped.
 *
 * Exercises the [com.wingedsheep.sdk.scripting.EntersTapped] replacement effect: casting the
 * creature from hand puts it onto the battlefield already tapped.
 */
class UnhallowedPhalanxScenarioTest : ScenarioTestBase() {

    init {
        context("Unhallowed Phalanx enters tapped") {

            test("casting it from hand puts it onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Unhallowed Phalanx")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unhallowed Phalanx").error shouldBe null
                game.resolveStack()

                val phalanx = game.findPermanent("Unhallowed Phalanx")
                withClue("Unhallowed Phalanx is on the battlefield") {
                    (phalanx != null) shouldBe true
                }
                withClue("It enters tapped") {
                    game.state.getEntity(phalanx!!)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
