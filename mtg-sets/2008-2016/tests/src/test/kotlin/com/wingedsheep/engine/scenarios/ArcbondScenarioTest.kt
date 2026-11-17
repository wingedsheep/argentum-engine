package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Arcbond (FRF #91).
 *
 * "Choose target creature. Whenever that creature is dealt damage this turn, it deals that much
 * damage to each other creature and each player."
 *
 * The rider fires every time the chosen creature is dealt damage this turn, never hits the chosen
 * creature itself, and still fires when the damage is lethal (2014-11-24 ruling) — the dead creature
 * deals the damage by last-known information.
 */
class ArcbondScenarioTest : ScenarioTestBase() {

    init {
        test("each hit on the chosen creature is passed on to each other creature and each player, even a lethal one") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Arcbond")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Arcbond", giant).error shouldBe null
            game.resolveStack()

            // First Shock: the Giant (3/3) survives and deals 2 to everything else.
            game.castSpell(1, "Shock", giant).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 18

            // Second Shock kills the Giant; the rider still fires and finishes off the Courser.
            game.castSpell(1, "Shock", giant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Centaur Courser") shouldBe true
            game.getLifeTotal(1) shouldBe 16
            game.getLifeTotal(2) shouldBe 16
        }
    }
}
