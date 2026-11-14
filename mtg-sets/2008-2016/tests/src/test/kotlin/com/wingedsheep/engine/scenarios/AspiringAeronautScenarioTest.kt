package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Aspiring Aeronaut (ORI #46) — {3}{U} Creature — Human Artificer, 1/2.
 *
 *   Flying
 *   When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 */
class AspiringAeronautScenarioTest : ScenarioTestBase() {

    init {
        context("Aspiring Aeronaut") {

            test("flies itself and its ETB creates one 1/1 flying Thopter artifact creature token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aspiring Aeronaut")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Aspiring Aeronaut")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val aeronaut = game.findPermanent("Aspiring Aeronaut")!!
                val projected = game.state.projectedState
                withClue("Aspiring Aeronaut has flying") {
                    projected.hasKeyword(aeronaut, Keyword.FLYING) shouldBe true
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
