package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cower in Fear (M13 #84) — {1}{B}{B} Instant.
 *   Creatures your opponents control get -1/-1 until end of turn.
 */
class CowerInFearScenarioTest : ScenarioTestBase() {
    init {
        test("shrinks only the opponent's creatures, killing their 1/1s") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Cower in Fear")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val (mine, theirs) = game.findPermanents("Grizzly Bears")
                .partition { it in game.state.projectedState.getBattlefieldControlledBy(game.player1Id) }

            val result = game.castSpell(1, "Cower in Fear")
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(theirs.single()) shouldBe 1
            game.state.projectedState.getToughness(theirs.single()) shouldBe 1
            withClue("the opponent's 1/1 dies") { game.isInGraveyard(2, "Savannah Lions") shouldBe true }
            withClue("your own creature is untouched") {
                game.state.projectedState.getPower(mine.single()) shouldBe 2
                game.state.projectedState.getToughness(mine.single()) shouldBe 2
            }
        }
    }
}
