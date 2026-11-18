package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spawn of Thraxes (JOU #112) — {5}{R}{R} Creature — Dragon 5/5.
 *
 *   Flying
 *   When this creature enters, it deals damage to any target equal to the number of Mountains you control.
 */
class SpawnOfThraxesScenarioTest : ScenarioTestBase() {
    init {
        test("its entry deals damage equal to your Mountains, counting only Mountains") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Spawn of Thraxes")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Spawn of Thraxes").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("7 Mountains you control (not your Forests, not the opponent's Mountains)") {
                game.getLifeTotal(2) shouldBe 13
            }
        }
    }
}
