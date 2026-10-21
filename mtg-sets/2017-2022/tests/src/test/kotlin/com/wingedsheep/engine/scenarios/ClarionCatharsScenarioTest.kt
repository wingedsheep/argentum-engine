package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Clarion Cathars (MID #14) — "When this creature enters, create a 1/1 white Human creature token."
 */
class ClarionCatharsScenarioTest : ScenarioTestBase() {

    init {
        test("entering creates a 1/1 white Human creature token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Clarion Cathars")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Clarion Cathars").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Clarion Cathars") shouldBe true
            val token = game.findPermanents("Human Token").single()
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.getColors(token) shouldContainExactly setOf(Color.WHITE.name)
            projected.hasSubtype(token, "Human") shouldBe true
            projected.getController(token) shouldBe game.player1Id
        }
    }
}
