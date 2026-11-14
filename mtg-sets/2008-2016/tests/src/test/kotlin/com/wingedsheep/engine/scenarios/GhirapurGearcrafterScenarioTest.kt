package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ghirapur Gearcrafter (ORI #149) — {2}{R} Creature — Human Artificer, 2/1.
 *
 *   When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 */
class GhirapurGearcrafterScenarioTest : ScenarioTestBase() {

    init {
        context("Ghirapur Gearcrafter") {

            test("its ETB creates one 1/1 flying Thopter artifact creature token; it doesn't fly itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ghirapur Gearcrafter")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Ghirapur Gearcrafter")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val gearcrafter = game.findPermanent("Ghirapur Gearcrafter")!!
                val projected = game.state.projectedState
                withClue("Ghirapur Gearcrafter itself has no flying") {
                    projected.hasKeyword(gearcrafter, Keyword.FLYING) shouldBe false
                }

                val thopters = game.findPermanents("Thopter Token")
                withClue("exactly one Thopter token was created") { thopters.size shouldBe 1 }
                val thopter = thopters.single()
                withClue("the Thopter is a 1/1 flying artifact creature") {
                    projected.getPower(thopter) shouldBe 1
                    projected.getToughness(thopter) shouldBe 1
                    projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
                    projected.isCreature(thopter) shouldBe true
                    projected.hasType(thopter, "ARTIFACT") shouldBe true
                    projected.hasSubtype(thopter, "Thopter") shouldBe true
                }
            }
        }
    }
}
