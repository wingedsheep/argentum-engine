package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tar Snare — {2}{B} Instant.
 * "Target creature gets -3/-2 until end of turn."
 */
class TarSnareScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Tar Snare")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("a 3/3 becomes 0/1 and survives") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.castSpell(1, "Tar Snare", giant)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(giant) shouldBe 0
            game.state.projectedState.getToughness(giant) shouldBe 1
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isInGraveyard(1, "Tar Snare") shouldBe true
        }

        test("a 2/2 drops to 0 toughness and dies") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Tar Snare", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
