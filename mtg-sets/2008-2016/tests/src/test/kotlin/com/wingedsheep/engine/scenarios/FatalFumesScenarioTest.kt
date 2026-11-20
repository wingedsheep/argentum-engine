package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fatal Fumes (DGM #24) — {3}{B} Instant.
 *
 *   Target creature gets -4/-2 until end of turn.
 */
class FatalFumesScenarioTest : ScenarioTestBase() {
    init {
        test("gives the target -4/-2 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Fatal Fumes")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val cast = game.castSpell(1, "Fatal Fumes", giant)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("Hill Giant (3/3) survives as a -1/1") {
                game.state.projectedState.getPower(giant) shouldBe -1
                game.state.projectedState.getToughness(giant) shouldBe 1
            }
        }

        test("kills a creature with toughness 2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Fatal Fumes")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Fatal Fumes", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
