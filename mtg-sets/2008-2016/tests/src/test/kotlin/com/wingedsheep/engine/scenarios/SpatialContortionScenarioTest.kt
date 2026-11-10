package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spatial Contortion (OGW) — {1}{C} Instant.
 * "Target creature gets +3/-3 until end of turn."
 */
class SpatialContortionScenarioTest : ScenarioTestBase() {
    init {
        fun board(land: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Spatial Contortion")
            .withLandsOnBattlefield(1, land, 2)
            .withCardOnBattlefield(2, "Serra Angel")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("gives target creature +3/-3") {
            val game = board("Wastes")
            val angel = game.findPermanent("Serra Angel")!!

            game.castSpell(1, "Spatial Contortion", angel).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(angel) shouldBe 7
            game.state.projectedState.getToughness(angel) shouldBe 1
        }

        test("the -3 toughness kills a 2/2") {
            val game = board("Wastes")

            game.castSpell(1, "Spatial Contortion", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("colored mana can't pay the {C} in its cost") {
            val game = board("Plains")

            game.castSpell(1, "Spatial Contortion", game.findPermanent("Serra Angel")).error shouldNotBe null
        }
    }
}
