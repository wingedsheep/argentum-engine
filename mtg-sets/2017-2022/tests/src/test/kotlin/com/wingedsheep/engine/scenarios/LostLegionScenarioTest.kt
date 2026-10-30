package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lost Legion (ELD #94) — {1}{B}{B} Creature — Spirit Knight 2/3.
 *
 * "When this creature enters, scry 2."
 */
class LostLegionScenarioTest : ScenarioTestBase() {

    init {
        context("Lost Legion") {

            test("ETB scry 2 looks at the top two cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lost Legion")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lost Legion").error shouldBe null
                game.resolveStack()

                val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                scry.options.size shouldBe 2
                game.selectCards(scry.options).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                game.state.getLibrary(game.player1Id).takeLast(2).toSet() shouldBe scry.options.toSet()
                game.isOnBattlefield("Lost Legion") shouldBe true
            }
        }
    }
}
