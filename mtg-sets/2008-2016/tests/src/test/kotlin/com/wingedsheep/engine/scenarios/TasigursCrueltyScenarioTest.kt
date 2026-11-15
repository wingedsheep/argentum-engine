package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tasigur's Cruelty (Fate Reforged #88) — {5}{B} Sorcery.
 *
 * "Delve. Each opponent discards two cards."
 */
class TasigursCrueltyScenarioTest : ScenarioTestBase() {
    init {
        test("the opponent chooses and discards two cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tasigur's Cruelty")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Tasigur's Cruelty").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player2Id
            val bears = game.findCardsInHand(2, "Grizzly Bears").single()
            val giant = game.findCardsInHand(2, "Hill Giant").single()
            game.selectCards(listOf(bears, giant)).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 1
            game.isInHand(2, "Forest") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Tasigur's Cruelty") shouldBe true
        }
    }
}
