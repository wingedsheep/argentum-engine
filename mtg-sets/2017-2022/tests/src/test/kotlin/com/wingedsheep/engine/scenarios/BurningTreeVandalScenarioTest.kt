package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Burning-Tree Vandal (RNA #94) — "Whenever this creature attacks, you may discard a card. If you
 * do, draw a card." (Riot is the shared `riot()` helper, covered by its own tests.)
 */
class BurningTreeVandalScenarioTest : ScenarioTestBase() {
    private fun attacking() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Burning-Tree Vandal")
        .withCardInHand(1, "Hill Giant")
        .withCardInLibrary(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()
        .also { it.declareAttackers(mapOf("Burning-Tree Vandal" to 2)).error shouldBe null }

    init {
        test("discarding a card on attack draws a card") {
            val game = attacking()
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            if (game.hasPendingDecision()) {
                game.selectCards(game.findCardsInHand(1, "Hill Giant")).error shouldBe null
            }
            game.resolveStack()

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("declining keeps the hand as it was") {
            val game = attacking()
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }
    }
}
