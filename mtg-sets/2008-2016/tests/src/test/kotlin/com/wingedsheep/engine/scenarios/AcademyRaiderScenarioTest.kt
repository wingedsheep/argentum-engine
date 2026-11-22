package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Academy Raider (M14 #124) — "Whenever this creature deals combat damage to a player, you may
 * discard a card. If you do, draw a card."
 */
class AcademyRaiderScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Academy Raider")
        .withCardInHand(1, "Mountain")
        .withCardInHand(1, "Forest")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.connect() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Academy Raider" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers()
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (state.pendingDecision !is YesNoDecision && guard++ < 20) resolveStack()
    }

    init {
        context("Academy Raider") {
            test("combat damage lets you discard a card to draw a card") {
                val game = buildGame()
                game.connect()
                game.getLifeTotal(2) shouldBe 19
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                val select = game.getPendingDecision()
                select.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(game.findCardsInHand(1, "Mountain")).error shouldBe null
                game.resolveStack()

                withClue("the Mountain was discarded and the Island drawn") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                    game.isInHand(1, "Island") shouldBe true
                    game.isInHand(1, "Forest") shouldBe true
                    game.handSize(1) shouldBe 2
                }
            }

            test("declining keeps the hand and draws nothing") {
                val game = buildGame()
                game.connect()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Mountain") shouldBe true
                game.isInHand(1, "Island") shouldBe false
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
