package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Kozilek, Butcher of Truth (ROE #6) — the "put into a graveyard from anywhere" trigger shuffles
 * its *owner's* whole graveyard into their library.
 */
class KozilekButcherOfTruthScenarioTest : ScenarioTestBase() {
    init {
        test("destroyed Kozilek shuffles its owner's graveyard, itself included, into their library") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Kozilek, Butcher of Truth")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInHand(2, "Terminate")
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Terminate", game.findPermanent("Kozilek, Butcher of Truth")!!).error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 0
            game.findCardsInLibrary(1, "Kozilek, Butcher of Truth").size shouldBe 1
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            // The opponent's graveyard is untouched (Terminate joins it).
            game.graveyardSize(2) shouldBe 2
        }

        test("discarded Kozilek triggers from the graveyard too") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Kozilek, Butcher of Truth")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(2, "Mind Rot")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 0
            game.findCardsInLibrary(1, "Kozilek, Butcher of Truth").size shouldBe 1
        }
    }
}
