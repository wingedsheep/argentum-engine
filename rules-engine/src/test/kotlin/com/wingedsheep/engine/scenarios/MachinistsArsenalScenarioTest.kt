package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Machinist's Arsenal. */
class MachinistsArsenalScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Machinist's Arsenal") {
            test("equipped Hero gets +2/+2 for each artifact its controller controls") {
                // One other artifact (Coral Sword) already in play; casting the Arsenal makes the
                // second artifact, so 2 artifacts → +4/+4 on the 1/1 Hero token = 5/5.
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Machinist's Arsenal")
                    .withCardOnBattlefield(1, "Coral Sword")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Machinist's Arsenal")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val hero = game.findPermanent("Hero Token")!!
                val projected = stateProjector.project(game.state)
                withClue("2 artifacts (Coral Sword + Arsenal) → 1/1 + (+4/+4) = 5/5") {
                    projected.getPower(hero) shouldBe 5
                    projected.getToughness(hero) shouldBe 5
                }
                withClue("Equipped Hero is an Artificer in addition to Hero") {
                    projected.hasSubtype(hero, "Artificer") shouldBe true
                }
            }
        }
    }
}
