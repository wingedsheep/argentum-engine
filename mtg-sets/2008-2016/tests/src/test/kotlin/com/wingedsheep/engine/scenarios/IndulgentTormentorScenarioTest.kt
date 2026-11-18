package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Indulgent Tormentor (M15 #101) — {3}{B}{B} 5/3 flying. "At the beginning of your upkeep, draw a
 * card unless target opponent sacrifices a creature of their choice or pays 3 life."
 *
 * The opponent picks a cost or lets the Tormentor's controller draw. Whichever question they
 * decline, the draw is the *controller's*, and 3 life can be paid at exactly 3 life (CR 119.4).
 */
class IndulgentTormentorScenarioTest : ScenarioTestBase() {

    private fun board(opponentLife: Int = 20, opponentCreature: Boolean = true): TestGame {
        var builder = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Indulgent Tormentor")
            .withLifeTotal(2, opponentLife)
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
        if (opponentCreature) builder = builder.withCardOnBattlefield(2, "Centaur Courser")
        repeat(5) { builder = builder.withCardInLibrary(1, "Swamp") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Swamp") }
        return builder.build()
    }

    /** Reach the upkeep, aim the trigger at the opponent, and resolve it to its first question. */
    private fun TestGame.resolveTrigger() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        if (getPendingDecision() is ChooseTargetsDecision) selectTargets(listOf(player2Id))
        resolveStack()
    }

    private fun TestGame.choose(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("the opponent answers") { decision.playerId shouldBe player2Id }
        val index = decision.options.indexOfFirst { it.startsWith(label, ignoreCase = true) }
        check(index >= 0) { "'$label' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        test("the opponent sacrifices a creature and no one draws") {
            val game = board()
            val myHand = game.handSize(1)
            game.resolveTrigger()

            game.choose("Sacrifice")
            val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            select.playerId shouldBe game.player2Id
            game.selectCards(listOf(game.findPermanent("Centaur Courser")!!))
            game.resolveStack()

            withClue("the creature was sacrificed") { game.isInGraveyard(2, "Centaur Courser") shouldBe true }
            withClue("the controller didn't draw") { game.handSize(1) shouldBe myHand }
            game.getLifeTotal(2) shouldBe 20
        }

        test("choosing the life payment, then refusing it, makes the controller draw — not the opponent") {
            val game = board()
            val myHand = game.handSize(1)
            val theirHand = game.handSize(2)
            game.resolveTrigger()

            game.choose("Pay 3 life")
            val pay = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            pay.playerId shouldBe game.player2Id
            game.answerYesNo(false)
            game.resolveStack()

            withClue("Indulgent Tormentor's controller drew") { game.handSize(1) shouldBe myHand + 1 }
            withClue("the opponent drew nothing") { game.handSize(2) shouldBe theirHand }
            game.getLifeTotal(2) shouldBe 20
        }

        test("letting the controller draw is an option of its own") {
            val game = board()
            val myHand = game.handSize(1)
            game.resolveTrigger()

            game.choose("Let Indulgent Tormentor's controller draw")
            game.resolveStack()

            game.handSize(1) shouldBe myHand + 1
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }

        test("an opponent at exactly 3 life may pay 3 life, and loses the game") {
            val game = board(opponentLife = 3, opponentCreature = false)
            val myHand = game.handSize(1)
            game.resolveTrigger()

            game.choose("Pay 3 life")
            game.answerYesNo(true)
            game.resolveStack()

            withClue("life paid down to 0 (CR 119.4)") { game.getLifeTotal(2) shouldBe 0 }
            withClue("the payment stopped the draw") { game.handSize(1) shouldBe myHand }
            withClue("and the state-based action ends the game") { game.state.winnerId shouldBe game.player1Id }
        }

        test("an opponent who can pay neither cost isn't asked, and the controller draws") {
            val game = board(opponentLife = 2, opponentCreature = false)
            val myHand = game.handSize(1)
            game.resolveTrigger()

            withClue("no question") { (game.getPendingDecision() is ChooseOptionDecision) shouldBe false }
            game.handSize(1) shouldBe myHand + 1
            game.getLifeTotal(2) shouldBe 2
        }
    }
}
