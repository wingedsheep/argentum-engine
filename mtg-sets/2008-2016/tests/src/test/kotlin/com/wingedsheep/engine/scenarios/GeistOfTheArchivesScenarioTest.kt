package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Geist of the Archives (EMN #62) — {2}{U} Creature — Spirit, 0/4.
 *
 *   Defender
 *   At the beginning of your upkeep, scry 1.
 *
 * The upkeep scry looks at exactly the top card; bottoming it changes the draw-step card.
 */
class GeistOfTheArchivesScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Geist of the Archives")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)
        .build()

    private fun TestGame.toUpkeepScry(): SelectCardsDecision {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
        return state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    init {
        test("scrying the top card to the bottom draws the next card instead") {
            val game = board()
            val island = game.findCardsInLibrary(1, "Island").single()
            val scry = game.toUpkeepScry()
            scry.options shouldBe listOf(island)

            game.selectCards(listOf(island)).error shouldBe null
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            game.isInHand(1, "Forest") shouldBe true
            game.isInHand(1, "Island") shouldBe false
        }

        test("keeping the top card draws it") {
            val game = board()
            game.toUpkeepScry()

            game.skipSelection().error shouldBe null
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            game.isInHand(1, "Island") shouldBe true
            game.isInHand(1, "Forest") shouldBe false
        }
    }
}
