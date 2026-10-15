package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Queza, Augur of Agonies (SNC #212) — "Whenever you draw a card, target opponent loses 1 life and
 * you gain 1 life."
 *
 * Pins the draw-step draw firing the trigger and the drain landing on the targeted opponent.
 */
class QuezaAugurOfAgoniesScenarioTest : ScenarioTestBase() {

    init {
        test("drawing a card drains the target opponent for 1") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Queza, Augur of Agonies")
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .withActivePlayer(1)
                .withTurnNumber(3)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(game.player2Id))
            }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
