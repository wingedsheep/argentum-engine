package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rollick of Abandon (JOU #108) — {3}{R}{R} Sorcery.
 *
 *   All creatures get +2/-2 until end of turn.
 */
class RollickOfAbandonScenarioTest : ScenarioTestBase() {
    init {
        test("every creature on both sides gets +2/-2, so 2-toughness creatures die") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Rollick of Abandon")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Rollick of Abandon").error shouldBe null
            game.resolveStack()

            val giant = game.findPermanent("Hill Giant")!!
            withClue("your own 3/3 is pumped to 5/1") {
                game.state.projectedState.getPower(giant) shouldBe 5
                game.state.projectedState.getToughness(giant) shouldBe 1
            }
            withClue("the opponent's 2/2 drops to 0 toughness and dies") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
