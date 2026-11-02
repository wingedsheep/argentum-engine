package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Choose up to five {P} worth of modes" (the Bloomburrow Seasons) is answered by simulating every
 * spend of the budget, not by repeating the cheapest mode. Cheapest-first cast Season of Loss as
 * five rounds of "each player sacrifices a creature" — throwing away our own board, and passing on
 * the three-pawprint drain even when it was lethal.
 */
class BudgetModalChoiceAiTest : ScenarioTestBase() {

    private fun seasonOfLossModes(builder: ScenarioBuilder): List<Int> {
        val game = builder
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withCardInHand(1, "Season of Loss")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            .build()
            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
        game.castSpell(1, "Season of Loss").error shouldBe null
        game.resolveStack()
        val decision = game.state.pendingDecision
        decision.shouldBeInstanceOf<BudgetModalDecision>()
        val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
        val response = ai.respondToDecision(game.state, decision)
        response.shouldBeInstanceOf<BudgetModalResponse>()
        return response.selectedModeIndices
    }

    init {
        test("Season of Loss takes the lethal drain instead of five sacrifice rounds") {
            // Six creature cards in our graveyard, the opponent at 6: {P}{P}{P} is lethal.
            var builder = scenario().withPlayers().withLifeTotal(2, 6)
            repeat(6) { builder = builder.withCardInGraveyard(1, "Grizzly Bears") }
            builder = builder.withCardOnBattlefield(1, "Craw Wurm").withCardOnBattlefield(2, "Grizzly Bears")

            val modes = seasonOfLossModes(builder)

            withClue("the drain (mode 2) is lethal: chose $modes") { modes shouldContain 2 }
        }

        test("Season of Loss does not sacrifice our board past the opponent's last creature") {
            // Two Craw Wurms against one Grizzly Bears: a second sacrifice round costs us a Wurm
            // and them nothing.
            val builder = scenario().withPlayers()
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")

            val modes = seasonOfLossModes(builder)

            withClue("at most one sacrifice round (mode 0): chose $modes") {
                modes.count { it == 0 } shouldBeLessThanOrEqual 1
            }
        }
    }
}
