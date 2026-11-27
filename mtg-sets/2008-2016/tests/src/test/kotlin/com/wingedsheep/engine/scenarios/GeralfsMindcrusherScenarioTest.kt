package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class GeralfsMindcrusherScenarioTest : ScenarioTestBase() {
    init {
        test("when it enters, target player mills five cards") {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Geralf's Mindcrusher")
                .withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(7) { builder.withCardInLibrary(2, "Grizzly Bears") }
            val game = builder.build()

            val result = game.castSpell(1, "Geralf's Mindcrusher")
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("expected the enters trigger to ask for a target player") {
                (decision is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.isOnBattlefield("Geralf's Mindcrusher") shouldBe true
            game.graveyardSize(2) shouldBe 5
            game.librarySize(2) shouldBe 2
        }
    }
}
