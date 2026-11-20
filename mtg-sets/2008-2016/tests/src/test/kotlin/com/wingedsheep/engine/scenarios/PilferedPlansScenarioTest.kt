package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pilfered Plans (DGM #90) — {1}{U}{B} Sorcery.
 *
 *   Target player mills two cards. Draw two cards.
 */
class PilferedPlansScenarioTest : ScenarioTestBase() {
    init {
        test("the target player mills two and the caster draws two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pilfered Plans")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Pilfered Plans", 2).error shouldBe null
            game.resolveStack()

            withClue("the opponent milled two cards") {
                game.librarySize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 2
            }
            withClue("the caster drew two cards, and milled none") {
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 1 // just Pilfered Plans itself
            }
        }
    }
}
