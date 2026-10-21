package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vivisection (MBS #38) — as an additional cost, sacrifice a creature. Draw three cards.
 */
class VivisectionScenarioTest : ScenarioTestBase() {

    init {
        test("sacrifices a creature as a cost and draws three cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Vivisection")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithAdditionalSacrifice(1, "Vivisection", "Grizzly Bears").error shouldBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.resolveStack()

            game.handSize(1) shouldBe 3
            game.librarySize(1) shouldBe 1
        }
    }
}
