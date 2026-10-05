package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.TragicLesson
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tragic Lesson (HOU #51) — "Draw two cards. Then discard a card unless you return a land you
 * control to its owner's hand."
 */
class TragicLessonScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TragicLesson)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Casts Tragic Lesson and passes until its resolution pauses for the land-return choice. */
    fun GameTestDriver.castLesson(): Int {
        val me = player1
        giveMana(me, Color.BLUE, 3)
        val card = putCardInHand(me, "Tragic Lesson")
        castSpell(me, card).outcome shouldBe Outcome.Done
        val handBeforeResolve = getHandSize(me)
        var guard = 0
        while (stackSize > 0 && pendingDecision == null && guard++ < 10) bothPass()
        return handBeforeResolve
    }

    test("returning a land you control avoids the discard") {
        val d = driver()
        val me = d.player1
        val land = d.putLandOnBattlefield(me, "Island")
        val handBefore = d.castLesson()

        val decision = d.pendingDecision as? SelectCardsDecision
            ?: error("Expected the land-return choice")
        withClue("the choice comes after the two draws (2017-07-14 ruling)") {
            d.getHandSize(me) shouldBe handBefore + 2
        }
        decision.options.contains(land) shouldBe true

        d.submitCardSelection(me, listOf(land))
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        withClue("the land is in hand and nothing was discarded") {
            d.state.getBattlefield().contains(land) shouldBe false
            d.getHand(me).contains(land) shouldBe true
            d.getHandSize(me) shouldBe handBefore + 3
            d.getGraveyardCardNames(me).filter { it == "Island" }.size shouldBe 0
        }
        d.getGraveyardCardNames(me).contains("Tragic Lesson") shouldBe true
    }

    test("declining the return makes you discard a card") {
        val d = driver()
        val me = d.player1
        val land = d.putLandOnBattlefield(me, "Island")
        val handBefore = d.castLesson()

        d.pendingDecision as? SelectCardsDecision ?: error("Expected the land-return choice")
        d.submitCardSelection(me, emptyList())

        val discard = d.pendingDecision as? SelectCardsDecision
            ?: error("Expected a discard selection")
        d.submitCardSelection(me, listOf(discard.options.first()))
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        withClue("the land stays, and one of the drawn-into hand cards was discarded") {
            d.state.getBattlefield().contains(land) shouldBe true
            d.getHandSize(me) shouldBe handBefore + 1
            d.getGraveyardCardNames(me).filter { it == "Island" }.size shouldBe 1
        }
    }

    test("an opponent's land is not a legal return") {
        val d = driver()
        val me = d.player1
        val theirs = d.putLandOnBattlefield(d.getOpponent(me), "Island")
        d.castLesson()

        when (val decision = d.pendingDecision) {
            is SelectCardsDecision -> decision.options.contains(theirs) shouldBe false
            else -> {}
        }
        d.state.getBattlefield().contains(theirs) shouldBe true
    }
})
