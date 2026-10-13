package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sphinx of Clear Skies (DMU #67) — combat damage to a player reveals the top X cards (X = domain),
 * the opponent separates them into two piles, and the controller puts one pile into hand and the
 * other into the graveyard.
 */
class SphinxOfClearSkiesScenarioTest : ScenarioTestBase() {

    init {
        context("Sphinx of Clear Skies") {

            test("domain sets X; opponent splits, controller picks the hand pile") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sphinx of Clear Skies")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Island")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sphinx of Clear Skies" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 15

                val split = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the opponent separates the piles") {
                    split.playerId shouldBe game.player2Id
                }
                withClue("X = 2 (Island + Swamp): only the top two cards are revealed") {
                    split.options.size shouldBe 2
                }
                val pileOne = split.options.first()
                val pileTwo = split.options.last()
                game.selectCards(listOf(pileOne)).error shouldBe null

                val pile = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                pile.playerId shouldBe game.player1Id
                game.submitDecision(OptionChosenResponse(pile.id, 1)).error shouldBe null
                game.resolveStack()

                withClue("pile 2 went to hand, pile 1 to the graveyard, the unrevealed card stays") {
                    game.state.getHand(game.player1Id).contains(pileTwo) shouldBe true
                    game.state.getGraveyard(game.player1Id).contains(pileOne) shouldBe true
                    game.librarySize(1) shouldBe 1
                }
            }
        }
    }
}
