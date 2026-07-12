package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Toxic Scorpion (VOW #224) — {1}{G} Creature — Scorpion, 1/1, deathtouch.
 *
 *   When this creature enters, another target creature you control gains deathtouch until end of turn.
 *
 * Exercises the ETB grant: the trigger targets another creature you control and grants it deathtouch.
 */
class ToxicScorpionScenarioTest : ScenarioTestBase() {

    init {
        context("Toxic Scorpion ETB grants deathtouch") {

            test("entering grants another creature you control deathtouch until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Toxic Scorpion")
                    .withCardOnBattlefield(1, "Grizzly Bears") // another creature you control
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears does not start with deathtouch") {
                    game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
                }

                game.castSpell(1, "Toxic Scorpion").error shouldBe null
                game.resolveStack() // creature enters → ETB trigger asks for a target

                val result = game.selectTargets(listOf(bears))
                withClue("Targeting another creature you control is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears gains deathtouch") {
                    game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
                }
            }
        }
    }
}
