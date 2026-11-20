package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Horizon Chimera — {2}{G}{U} Creature — Chimera 3/2 (THS #194).
 *
 * "Flash / Flying, trample / Whenever you draw a card, you gain 1 life."
 */
class HorizonChimeraScenarioTest : ScenarioTestBase() {

    init {
        test("gains 1 life for each card its controller draws, not for the opponent's draws") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Horizon Chimera")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            withClue("two cards drawn -> two separate 1-life triggers") { game.getLifeTotal(1) shouldBe 22 }
            withClue("the opponent's life is untouched") { game.getLifeTotal(2) shouldBe 20 }
        }
    }
}
