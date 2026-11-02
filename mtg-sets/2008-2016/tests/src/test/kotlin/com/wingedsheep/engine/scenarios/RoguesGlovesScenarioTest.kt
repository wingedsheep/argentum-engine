package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rogue's Gloves (M15 #227) — "Whenever equipped creature deals combat damage to a player, you may
 * draw a card."
 */
class RoguesGlovesScenarioTest : ScenarioTestBase() {
    init {
        test("connecting with the equipped creature lets you draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Rogue's Gloves", "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()
            game.hasPendingDecision() shouldBe true // the "you may" prompt
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 0
        }
    }
}
