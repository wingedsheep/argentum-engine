package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.TamiyoMeetsTheStoryCircle
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tamiyo Meets the Story Circle (MH3 #72) — {1}{U} Enchantment — Saga.
 *
 *  I — Until your next turn, whenever a creature attacks you or a planeswalker you control, it gets
 *      -2/-0 until end of turn.
 *  II — Discard any number of cards, then investigate twice for each card discarded this way.
 *  III — Shuffle up to three target cards from your graveyard into your library.
 */
class TamiyoMeetsTheStoryCircleScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TamiyoMeetsTheStoryCircle, PredefinedTokens.Clue))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    /**
     * Pass until turn [turn] reaches [step] (stopping as soon as a target choice is pending),
     * auto-answering incidental decisions such as cleanup discards.
     */
    fun GameTestDriver.advanceTo(turn: Int, step: Step) {
        var guard = 0
        while (!(state.turnNumber == turn && state.step == step) && guard++ < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $turn $step")
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> return
                decision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.clues(player: EntityId): Int =
        getPermanents(player).count { getCardName(it) == "Clue" }

    fun GameTestDriver.castSaga(player: EntityId) {
        giveMana(player, Color.BLUE, 2)
        val saga = putCardInHand(player, "Tamiyo Meets the Story Circle")
        castSpell(player, saga)
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("I shrinks attackers until your next turn, II investigates twice per discard, III shuffles up to three") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSaga(me)
        driver.findPermanent(me, "Tamiyo Meets the Story Circle") shouldNotBe null

        // Opponent's turn: Centaur Courser attacks me.
        driver.advanceTo(2, Step.DECLARE_ATTACKERS)
        driver.state.activePlayerId shouldBe opponent
        driver.declareAttackers(opponent, listOf(courser), me)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        withClue("chapter I's watcher gives the attacking Courser -2/-0") {
            driver.state.projectedState.getPower(courser) shouldBe 1
            driver.state.projectedState.getToughness(courser) shouldBe 3
        }

        // My turn 3: lore 2 → chapter II asks which cards to discard.
        driver.advanceTo(3, Step.PRECOMBAT_MAIN)
        withClue("the shrunken Courser dealt only 1 damage") { driver.getLifeTotal(me) shouldBe 19 }
        withClue("the -2/-0 lasted only until end of turn") {
            driver.state.projectedState.getPower(courser) shouldBe 3
        }
        guard = 0
        while (driver.state.pendingDecision !is SelectCardsDecision && guard++ < 10) driver.bothPass()
        val discard = driver.getHand(me).take(2)
        driver.submitCardSelection(me, discard)
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        withClue("two discarded cards → investigate four times") {
            driver.clues(me) shouldBe 4
            discard.forEach { driver.getGraveyard(me) shouldContain it }
        }

        // Turn 4 (opponent): the chapter I watcher expired at the start of my turn 3.
        driver.advanceTo(4, Step.DECLARE_ATTACKERS)
        driver.declareAttackers(opponent, listOf(courser), me)
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        withClue("until your next turn — the watcher no longer fires") {
            driver.state.projectedState.getPower(courser) shouldBe 3
        }

        // My turn 5: lore 3 → chapter III targets up to three cards in my graveyard.
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")
        driver.advanceTo(5, Step.PRECOMBAT_MAIN)
        guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) driver.bothPass()
        val chosen = listOf(bears, lions, discard[0])
        driver.submitTargetSelection(me, chosen)
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        withClue("the three targets are shuffled into my library; the fourth card stays") {
            chosen.forEach {
                driver.state.getLibrary(me) shouldContain it
                driver.getGraveyard(me) shouldNotContain it
            }
            driver.getGraveyard(me) shouldContain discard[1]
        }
        withClue("the Saga is sacrificed after chapter III") {
            driver.findPermanent(me, "Tamiyo Meets the Story Circle") shouldBe null
        }
    }

    test("II with no discards investigates zero times") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSaga(me)

        driver.advanceTo(3, Step.PRECOMBAT_MAIN)
        var guard = 0
        while (driver.state.pendingDecision !is SelectCardsDecision && guard++ < 10) driver.bothPass()
        val handBefore = driver.getHandSize(me)
        driver.submitCardSelection(me, emptyList())
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.getHandSize(me) shouldBe handBefore
        driver.clues(me) shouldBe 0
    }
})
