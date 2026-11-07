package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Purple-Crystal Crab (Global Series Jiang Yanggu & Mu Yanling #3) — {1}{U} Creature — Crab 1/1.
 * "When this creature dies, draw a card."
 */
class PurpleCrystalCrabScenarioTest : ScenarioTestBase() {
    init {
        test("dying draws its controller a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Purple-Crystal Crab")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val crab = game.findPermanent("Purple-Crystal Crab")!!

            game.castSpell(2, "Shock", crab).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Purple-Crystal Crab") shouldBe true
            game.isInHand(1, "Island") shouldBe true
        }
    }
}
