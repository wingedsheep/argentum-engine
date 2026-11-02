package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Psychic Symbiont (M19 #221) — "When this creature enters, target opponent discards a card and
 * you draw a card."
 */
class PsychicSymbiontScenarioTest : ScenarioTestBase() {
    init {
        test("entering makes the opponent discard and you draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Psychic Symbiont")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Island")
                .withCardInHand(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Psychic Symbiont").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                val bears = game.findCardsInHand(2, "Grizzly Bears")
                if (bears.isNotEmpty()) game.selectCards(bears)
                game.resolveStack()
            }

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 0
        }
    }
}
