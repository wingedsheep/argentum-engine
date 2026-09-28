package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Shatter the Source — modal: 6 damage to creature/planeswalker/battle, or destroy target artifact.
 */
class ShatterTheSourceScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Shatter the Source")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Ornithopter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("mode one deals 6 damage to a creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpellWithMode(1, "Shatter the Source", 0, bears).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("mode two destroys an artifact") {
            val game = board()
            val orni = game.findPermanent("Ornithopter")!!
            game.castSpellWithMode(1, "Shatter the Source", 1, orni).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Ornithopter") shouldBe true
        }
    }
}
