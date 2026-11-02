package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hieromancer's Cage (M19 #14) — exiles target nonland permanent an opponent controls until the
 * Cage leaves the battlefield.
 */
class HieromancersCageScenarioTest : ScenarioTestBase() {
    init {
        test("exiles an opponent's creature and returns it when the Cage is destroyed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hieromancer's Cage")
                .withCardInHand(1, "Naturalize")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hieromancer's Cage").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
                game.resolveStack()
            }
            game.isInExile(2, "Hill Giant") shouldBe true

            game.castSpell(1, "Naturalize", game.findPermanent("Hieromancer's Cage")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Hieromancer's Cage") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
