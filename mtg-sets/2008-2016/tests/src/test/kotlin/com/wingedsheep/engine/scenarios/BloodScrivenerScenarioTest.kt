package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Blood Scrivener — "If you would draw a card while you have no cards in hand, instead you draw
 * two cards and you lose 1 life."
 *
 * The replacement's own draws must not be replaced again by the same Scrivener (CR 614.5), while a
 * second Scrivener applies to the first of them (the card's 2013-04-15 ruling).
 */
class BloodScrivenerScenarioTest : ScenarioTestBase() {

    init {
        fun board(scriveners: Int, cardInHand: Boolean): TestGame {
            var builder = scenario()
                .withPlayers()
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(scriveners) { builder = builder.withCardOnBattlefield(1, "Blood Scrivener") }
            if (cardInHand) builder = builder.withCardInHand(1, "Swamp")
            return builder.build()
        }

        test("an empty-hand draw becomes two draws and 1 life lost") {
            val game = board(scriveners = 1, cardInHand = false)
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()
            game.handSize(1) shouldBe 2
            game.getLifeTotal(1) shouldBe 19
        }

        test("a draw with cards in hand is unchanged") {
            val game = board(scriveners = 1, cardInHand = true)
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()
            game.handSize(1) shouldBe 2 // 1 held + 1 drawn
            game.getLifeTotal(1) shouldBe 20
        }

        test("two Scriveners are cumulative: three cards and 2 life lost") {
            val game = board(scriveners = 2, cardInHand = false)
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            // Two replacements apply to the draw: the player picks which applies first (CR 616.1),
            // then the other applies to the first of the two replacement draws.
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, optionIndex = 0)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 3
            game.getLifeTotal(1) shouldBe 18
        }
    }
}
