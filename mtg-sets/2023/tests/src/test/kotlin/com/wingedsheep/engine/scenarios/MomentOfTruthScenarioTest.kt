package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Moment of Truth (March of the Machine #67): top three — one to hand, one to graveyard, one to
 * the bottom of the library.
 */
class MomentOfTruthScenarioTest : ScenarioTestBase() {

    init {
        test("splits the top three cards between hand, graveyard, and library bottom") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Moment of Truth")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Giant Growth")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val library = game.state.getLibrary(game.player1Id)
            val top3 = library.take(3)
            val fourth = library[3]

            game.castSpell(1, "Moment of Truth").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(top3[1]))
            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(top3[2]))

            game.state.getHand(game.player1Id).contains(top3[1]) shouldBe true
            game.state.getGraveyard(game.player1Id).contains(top3[2]) shouldBe true

            val after = game.state.getLibrary(game.player1Id)
            after.size shouldBe 2
            after.first() shouldBe fourth
            after.last() shouldBe top3[0]
        }
    }
}
