package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Decimator of the Provinces — {10} 7/7 Eldrazi Boar with trample, haste,
 * emerge {6}{G}{G}{G}, and "When you cast this spell, creatures you control get +2/+2 and gain
 * trample until end of turn."
 *
 * Because the pump is a cast trigger it resolves while the Boar is still on the stack, so the Boar
 * itself is not among "creatures you control" and stays a 7/7.
 */
class DecimatorOfTheProvincesScenarioTest : ScenarioTestBase() {

    init {
        context("Decimator of the Provinces") {

            test("emerge cast pumps the team but not the Boar, which is still on the stack") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Decimator of the Provinces")
                    .withCardOnBattlefield(1, "Force of Nature") // {3}{G}{G} → mana value 5, 5/5
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2, stays behind to be pumped
                    // Emerge {6}{G}{G}{G} reduced by 5 → {1}{G}{G}{G}: four Forests.
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .build()

                val cast = game.castSpellWithEmerge(
                    1, "Decimator of the Provinces", "Force of Nature",
                )
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Force of Nature") shouldBe true

                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the surviving creature got +2/+2 and trample") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }

                val boar = game.findPermanent("Decimator of the Provinces")!!
                withClue("the Boar was on the stack when the trigger resolved, so it is still 7/7") {
                    game.state.projectedState.getPower(boar) shouldBe 7
                    game.state.projectedState.getToughness(boar) shouldBe 7
                }
                withClue("it brings its own trample and haste") {
                    game.state.projectedState.hasKeyword(boar, Keyword.TRAMPLE) shouldBe true
                    game.state.projectedState.hasKeyword(boar, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
