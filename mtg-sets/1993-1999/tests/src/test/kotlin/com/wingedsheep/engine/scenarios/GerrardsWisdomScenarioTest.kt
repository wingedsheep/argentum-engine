package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class GerrardsWisdomScenarioTest : ScenarioTestBase() {
    init {
        test("counts cards in hand at resolution after a response draws more cards") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Gerrard's Wisdom")
                .withCardInHand(1, "Ancestral Recall")
                .withCardInHand(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gerrard's Wisdom").error shouldBe null
            game.castSpellTargetingPlayer(1, "Ancestral Recall", 1).error shouldBe null
            game.resolveStack()

            // Wisdom and Recall are no longer in hand: one original card plus three drawn cards.
            game.state.getHand(game.player1Id).size shouldBe 4
            game.getLifeTotal(1) shouldBe 28
            game.getLifeTotal(2) shouldBe 20
        }

        test("gains no life with an empty hand and does not count itself on the stack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Gerrard's Wisdom")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gerrard's Wisdom").error shouldBe null
            game.resolveStack()

            game.state.getHand(game.player1Id).size shouldBe 0
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
