package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cloaked Siren (JOU #32) — {3}{U} Creature — Siren 3/2, Flash, Flying.
 */
class CloakedSirenScenarioTest : ScenarioTestBase() {
    init {
        test("flash: it can be cast on the opponent's turn and enters as a flier") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Cloaked Siren")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("casting on the opponent's turn is allowed") {
                game.castSpell(1, "Cloaked Siren").error shouldBe null
            }
            game.resolveStack()

            val siren = game.findPermanent("Cloaked Siren")
            withClue("the Siren resolved onto the battlefield") {
                (siren != null) shouldBe true
            }
            game.state.projectedState.hasKeyword(siren!!, Keyword.FLYING) shouldBe true
        }
    }
}
