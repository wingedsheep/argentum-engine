package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gravity Sphere (LEG) — {2}{R} World Enchantment: "All creatures lose flying."
 */
class GravitySphereScenarioTest : ScenarioTestBase() {

    init {
        context("Gravity Sphere") {
            test("creatures on both sides lose flying while it is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gravity Sphere")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Serra Angel")!!, Keyword.FLYING) shouldBe false
                projected.hasKeyword(game.findPermanent("Air Elemental")!!, Keyword.FLYING) shouldBe false
                projected.hasKeyword(game.findPermanent("Serra Angel")!!, Keyword.VIGILANCE) shouldBe true
            }

            test("the world rule: a newer world enchantment replaces it and creatures fly again") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gravity Sphere")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withCardInHand(1, "Storm World")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Storm World")
                game.resolveStack()

                game.isInGraveyard(1, "Gravity Sphere") shouldBe true
                game.isOnBattlefield("Storm World") shouldBe true
                game.state.projectedState.hasKeyword(game.findPermanent("Air Elemental")!!, Keyword.FLYING) shouldBe true
            }
        }
    }
}
