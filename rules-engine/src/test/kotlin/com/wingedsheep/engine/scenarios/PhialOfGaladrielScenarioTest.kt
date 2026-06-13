package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Phial of Galadriel — conditional draw and life-gain replacements:
 *  - "If you would draw a card while you have no cards in hand, draw two cards instead."
 *  - "If you would gain life while you have 5 or less life, you gain twice that much life instead."
 * Exercises the new `restrictions` gate on `ModifyLifeGain` (and the existing `ModifyDrawAmount`).
 */
class PhialOfGaladrielScenarioTest : ScenarioTestBase() {

    init {
        test("draws two when drawing with an empty hand") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.DRAW) // P1's turn-based draw, hand empty
            game.resolveStack()
            game.handSize(1) shouldBe 2
        }

        test("draws only one when hand is not empty") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardInHand(1, "Plains")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()
            game.handSize(1) shouldBe 2 // 1 held + 1 drawn (no doubling), not 3
        }

        test("doubles life gain while at 5 or less life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardInHand(1, "Reviving Dose")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLifeTotal(1, 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Reviving Dose").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 11 // 5 + (3 * 2)
        }

        test("does not double life gain above 5 life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardInHand(1, "Reviving Dose")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLifeTotal(1, 10)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Reviving Dose").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 13 // 10 + 3 (no doubling)
        }
    }
}
