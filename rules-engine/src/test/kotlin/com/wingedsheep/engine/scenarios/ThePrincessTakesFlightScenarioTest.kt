package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.ThePrincessTakesFlight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Princess Takes Flight (WOE) — {2}{W} Enchantment — Saga.
 *
 * I — Exile up to one target creature.
 * II — Target creature you control gets +2/+2 and gains flying until end of turn.
 * III — Return the exiled card to the battlefield under its owner's control.
 *
 * The blink is held by the Saga's linked exile, so the tests pin the two things that depends on:
 * chapter III returns the card to its *owner*, not to the Saga's controller, and a chapter I that
 * exiled nothing (the target is "up to one") leaves chapter III with nothing to return.
 */
class ThePrincessTakesFlightScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThePrincessTakesFlight))
        return driver
    }

    /** Drain the stack, auto-answering anything that pauses. */
    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /** Drain the stack, answering every target request with [targets]. */
    fun GameTestDriver.drainTargeting(chooser: EntityId, targets: List<EntityId>) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> submitTargetSelection(chooser, targets)
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    /** Advance to the precombat main of the starting player's [nth] turn (turn `2n - 1` in a duel). */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn precombat main")
    }

    test("exiles an opponent's creature and hands it back to them on chapter III") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val ours = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.WHITE, 3)
        val saga = driver.putCardInHand(controller, "The Princess Takes Flight")
        driver.castSpell(controller, saga)
        driver.drainTargeting(controller, listOf(bears))

        withClue("chapter I exiles the targeted creature") {
            driver.getPermanents(opponent).contains(bears) shouldBe false
        }

        driver.advanceToMain(2)
        driver.drainTargeting(controller, listOf(ours))

        withClue("chapter II pumps our own creature and gives it flying until end of turn") {
            driver.state.projectedState.getPower(ours) shouldBe 4
            driver.state.projectedState.getToughness(ours) shouldBe 4
            driver.state.projectedState.hasKeyword(ours, Keyword.FLYING) shouldBe true
        }

        driver.advanceToMain(3)
        driver.drain()

        withClue("chapter III returns the exiled card under its OWNER's control, not ours") {
            driver.getPermanents(opponent).count { driver.getCardName(it) == "Grizzly Bears" } shouldBe 1
            driver.getPermanents(controller).count { driver.getCardName(it) == "Grizzly Bears" } shouldBe 1
        }
        withClue("a three-chapter Saga is sacrificed after chapter III") {
            driver.getGraveyardCardNames(controller).contains("The Princess Takes Flight") shouldBe true
        }
    }

    test("chapter I with no target leaves chapter III with nothing to return") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.WHITE, 3)
        val saga = driver.putCardInHand(controller, "The Princess Takes Flight")
        driver.castSpell(controller, saga)
        // No creatures anywhere, so "up to one target creature" is satisfied by choosing none.
        driver.drainTargeting(controller, emptyList())

        driver.advanceToMain(2)
        driver.drainTargeting(controller, emptyList())
        driver.advanceToMain(3)
        driver.drain()

        withClue("nothing was exiled, so nothing comes back and the Saga still finishes") {
            driver.getPermanents(controller).none { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
            driver.getGraveyardCardNames(controller).contains("The Princess Takes Flight") shouldBe true
        }
    }
})
