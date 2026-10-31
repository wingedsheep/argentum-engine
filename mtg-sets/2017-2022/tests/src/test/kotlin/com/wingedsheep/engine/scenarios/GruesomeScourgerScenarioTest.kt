package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gruesome Scourger (M20 #104) — "When this creature enters, it deals damage to target opponent
 * or planeswalker equal to the number of creatures you control."
 *
 * The count includes the Scourger itself and ignores the opponent's creatures.
 */
class GruesomeScourgerScenarioTest : ScenarioTestBase() {

    init {
        test("ETB deals damage equal to the creatures you control, counting itself") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gruesome Scourger")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lifeBefore = game.getLifeTotal(2)

            game.castSpell(1, "Gruesome Scourger").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldNotBeNull()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe lifeBefore - 3
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
