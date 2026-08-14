package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sharpened Pitchfork (ISD #232) — Equipment granting first strike unconditionally, and +1/+1 only
 * while the equipped creature is a Human.
 */
class SharpenedPitchforkScenarioTest : ScenarioTestBase() {
    init {
        test("equipped Human gets first strike and +1/+1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardAttachedTo(1, "Sharpened Pitchfork", "Glory Seeker")
                .build()

            val human = game.findPermanent("Glory Seeker")!!
            withClue("Glory Seeker is a 2/2 Human — the Human clause applies") {
                game.state.projectedState.getPower(human) shouldBe 3
                game.state.projectedState.getToughness(human) shouldBe 3
            }
            game.state.projectedState.hasKeyword(human, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("equipped non-Human gets first strike but no bonus") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Sharpened Pitchfork", "Grizzly Bears")
                .build()

            val bear = game.findPermanent("Grizzly Bears")!!
            withClue("first strike is unconditional") {
                game.state.projectedState.hasKeyword(bear, Keyword.FIRST_STRIKE) shouldBe true
            }
            withClue("the +1/+1 is gated on Human, so a 2/2 Bear stays 2/2") {
                game.state.projectedState.getPower(bear) shouldBe 2
                game.state.projectedState.getToughness(bear) shouldBe 2
            }
        }
    }
}
