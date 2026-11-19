package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Spirit of the Labyrinth: "Each player can't draw more than one card each turn."
 * The cap binds its own controller too — a draw-two spell draws only one card.
 */
class SpiritOfTheLabyrinthScenarioTest : ScenarioTestBase() {

    init {
        test("its controller draws only one card from a draw-two spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Spirit of the Labyrinth")
                .withCardInHand(1, "Counsel of the Soratami")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.castSpell(1, "Counsel of the Soratami").error shouldBe null
            game.resolveStack()

            // Counsel left the hand, one card was drawn.
            game.handSize(1) shouldBe handBefore
        }
    }
}
