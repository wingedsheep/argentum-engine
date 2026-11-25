package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wit's End (DIS #58) — {5}{B}{B} Sorcery.
 *   Target player discards their hand.
 */
class WitsEndScenarioTest : ScenarioTestBase() {
    init {
        test("target player discards their whole hand") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Wit's End")
                .withCardInHand(1, "Grizzly Bears")
                .withCardsInHand(2, "Grizzly Bears", 2)
                .withCardInHand(2, "Forest")
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpellTargetingPlayer(1, "Wit's End", 2)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.handSize(2) shouldBe 0
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Forest") shouldBe true
            // The caster's own hand is untouched.
            game.handSize(1) shouldBe 1
        }
    }
}
