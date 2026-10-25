package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Toph, Greatest Earthbender ({2}{R}{G}, 3/3):
 * "When Toph enters, earthbend X, where X is the amount of mana spent to cast her.
 *  Land creatures you control have double strike."
 *
 * Cast off a Mountain, a Forest and two Wastes, so the generic half is paid with colorless mana:
 * X is the full 4 spent, not just her colored pips.
 */
class TophGreatestEarthbenderScenarioTest : ScenarioTestBase() {
    init {
        test("earthbends X = all mana spent, and the land creature has double strike") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Toph, Greatest Earthbender")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Snow-Covered Wastes", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Toph, Greatest Earthbender").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val forest = game.findPermanent("Forest")!!
            game.selectTargets(listOf(forest)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.hasType(forest, "CREATURE") shouldBe true
            projected.getPower(forest) shouldBe 4
            projected.getToughness(forest) shouldBe 4
            projected.hasKeyword(forest, Keyword.DOUBLE_STRIKE) shouldBe true
            projected.hasKeyword(game.findPermanent("Toph, Greatest Earthbender")!!, Keyword.DOUBLE_STRIKE) shouldBe false
        }
    }
}
