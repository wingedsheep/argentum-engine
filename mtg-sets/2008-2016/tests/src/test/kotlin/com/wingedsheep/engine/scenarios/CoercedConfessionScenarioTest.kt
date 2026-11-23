package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Coerced Confession (GTC #217) — "Target player mills four cards. You draw a card for each creature
 * card put into their graveyard this way."
 *
 * First card to run the pipeline mill against `Player.TargetPlayer`: proves the targeted player is the
 * one milled, and that only the creature cards this resolution milled are counted.
 */
class CoercedConfessionScenarioTest : ScenarioTestBase() {
    init {
        test("the target opponent mills four and you draw one card per creature milled") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Coerced Confession")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpellTargetingPlayer(1, "Coerced Confession", 2)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.librarySize(2) shouldBe 0
            game.graveyardSize(2) shouldBe 5
            withClue("two creatures milled, the one already in the graveyard isn't counted") {
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 1
            }
        }

        test("no creatures milled means no cards drawn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Coerced Confession")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Coerced Confession", 2).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 2
            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 1
        }
    }
}
