package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Psychic Corrosion (M19 #68) — "Whenever you draw a card, each opponent mills two cards."
 * Each card drawn is its own trigger, so a draw-two mills four.
 */
class PsychicCorrosionScenarioTest : ScenarioTestBase() {
    init {
        test("each card you draw mills the opponent two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Psychic Corrosion")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            game.librarySize(2) shouldBe 1
            game.graveyardSize(2) shouldBe 4
            game.graveyardSize(1) shouldBe 1 // only Divination — you don't mill yourself
        }
    }
}
