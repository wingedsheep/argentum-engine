package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rakdos Ringleader (RTR) — Whenever this creature deals combat damage to a player, that player
 * discards a card at random.
 */
class RakdosRingleaderScenarioTest : ScenarioTestBase() {
    init {
        test("unblocked combat damage makes the opponent discard exactly one card at random") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Rakdos Ringleader")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Rakdos Ringleader" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("3 first-strike damage dealt and one random card discarded") {
                game.getLifeTotal(2) shouldBe 17
                game.handSize(2) shouldBe 2
                game.graveyardSize(2) shouldBe 1
            }
        }
    }
}
