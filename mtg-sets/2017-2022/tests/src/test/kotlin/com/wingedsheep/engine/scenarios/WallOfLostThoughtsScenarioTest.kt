package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Wall of Lost Thoughts (RNA #59) — Defender; ETB: target player mills four cards.
 */
class WallOfLostThoughtsScenarioTest : ScenarioTestBase() {
    init {
        test("the ETB mills the target player for four") {
            var builder = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Wall of Lost Thoughts")
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
            val game = builder.build()

            game.castSpell(1, "Wall of Lost Thoughts").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldNotBeNull()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 4
            game.librarySize(2) shouldBe 1
        }
    }
}
