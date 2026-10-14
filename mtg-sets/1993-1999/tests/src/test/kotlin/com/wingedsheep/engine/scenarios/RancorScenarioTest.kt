package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rancor (ULG #110) — {G} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+0 and has trample.
 *   When this Aura is put into a graveyard from the battlefield, return it to its owner's hand.
 */
class RancorScenarioTest : ScenarioTestBase() {

    init {
        test("enchanted creature gets +2/+0 and trample") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Rancor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Rancor", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Rancor") shouldBe true
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 2
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
        }

        test("when the enchanted creature is destroyed, Rancor returns to its owner's hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Rancor")
                .withCardInHand(1, "Doom Blade")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Rancor", bears).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Rancor") shouldBe true

            game.castSpell(1, "Doom Blade", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Rancor") shouldBe false
            game.isInGraveyard(1, "Rancor") shouldBe false
            game.isInHand(1, "Rancor") shouldBe true
        }
    }
}
