package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Advent of the Wurm (DGM #51) — {1}{G}{G}{W} Instant.
 *
 *   Create a 5/5 green Wurm creature token with trample.
 */
class AdventOfTheWurmScenarioTest : ScenarioTestBase() {
    init {
        test("creates a 5/5 green Wurm token with trample") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Advent of the Wurm")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Advent of the Wurm")
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val wurm = game.findPermanents("Wurm Token").single()
            val projected = game.state.projectedState
            projected.getPower(wurm) shouldBe 5
            projected.getToughness(wurm) shouldBe 5
            projected.hasKeyword(wurm, Keyword.TRAMPLE) shouldBe true
            game.isInGraveyard(1, "Advent of the Wurm") shouldBe true
        }
    }
}
