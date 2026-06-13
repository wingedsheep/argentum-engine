package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gandalf's Sanction — "deals X damage to target creature, where X is the number of instant and
 * sorcery cards in your graveyard. Excess damage is dealt to that creature's controller instead."
 * Exercises the new `DealDamageExcessToController` (excess beyond lethal goes to the controller).
 */
class GandalfsSanctionScenarioTest : ScenarioTestBase() {

    init {
        test("X = instants/sorceries in graveyard; excess goes to the creature's controller") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Gandalf's Sanction")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInGraveyard(1, "Glorious Gale")   // instant
                .withCardInGraveyard(1, "Claim the Precious") // sorcery
                .withCardInGraveyard(1, "Many Partings")   // sorcery
                .withCardInGraveyard(1, "Ringsight")       // sorcery  → X = 4
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Gandalf's Sanction", bears).error shouldBe null
            game.resolveStack()

            // 4 damage to a 2/2: lethal 2 kills it; the 2 excess hits its controller (20 → 18).
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
