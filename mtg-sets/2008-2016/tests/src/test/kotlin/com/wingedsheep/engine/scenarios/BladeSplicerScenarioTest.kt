package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Blade Splicer (NPH #4) — {2}{W} Creature — Phyrexian Human Artificer, 1/1.
 *
 *   When this creature enters, create a 3/3 colorless Phyrexian Golem artifact creature token.
 *   Golems you control have first strike.
 */
class BladeSplicerScenarioTest : ScenarioTestBase() {

    init {
        context("Blade Splicer") {

            test("creates a 3/3 Golem token that has first strike; non-Golems and opponents' Golems don't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blade Splicer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Cobalt Golem")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Blade Splicer")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val golems = game.findPermanents("Phyrexian Golem Token")
                withClue("exactly one Phyrexian Golem token was created") { golems.size shouldBe 1 }
                val golem = golems.single()
                val projected = game.state.projectedState
                withClue("the Golem is a 3/3 artifact creature") {
                    projected.getPower(golem) shouldBe 3
                    projected.getToughness(golem) shouldBe 3
                    projected.isCreature(golem) shouldBe true
                    projected.hasType(golem, "ARTIFACT") shouldBe true
                }
                withClue("Golems you control have first strike") {
                    projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("non-Golems (Blade Splicer itself, the Bears) don't") {
                    projected.hasKeyword(game.findPermanent("Blade Splicer")!!, Keyword.FIRST_STRIKE) shouldBe false
                    projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FIRST_STRIKE) shouldBe false
                }
                withClue("an opponent's Golem doesn't") {
                    projected.hasKeyword(game.findPermanent("Cobalt Golem")!!, Keyword.FIRST_STRIKE) shouldBe false
                }
            }
        }
    }
}
