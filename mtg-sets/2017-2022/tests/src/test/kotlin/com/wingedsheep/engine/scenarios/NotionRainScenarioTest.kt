package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Notion Rain (GRN #193) — {1}{U}{B} Sorcery.
 *
 *   Surveil 2, then draw two cards. Notion Rain deals 2 damage to you.
 */
class NotionRainScenarioTest : ScenarioTestBase() {

    init {
        test("surveils the top two away, draws the next two, and deals 2 damage to its caster") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Notion Rain")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Notion Rain").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 2
            game.selectCards(decision.options).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
            game.resolveStack()

            game.librarySize(1) shouldBe 0
            game.handSize(1) shouldBe 2
            // two surveiled cards + Notion Rain itself
            game.graveyardSize(1) shouldBe 3
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
