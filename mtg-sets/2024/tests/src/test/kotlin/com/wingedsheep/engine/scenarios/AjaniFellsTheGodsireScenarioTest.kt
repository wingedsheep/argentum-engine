package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.AjaniFellsTheGodsire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
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
 * Ajani Fells the Godsire — {3}{W}{W} Enchantment — Saga.
 *
 *  I — Exile target creature an opponent controls with power 3 or greater.
 *  II — Create a 2/1 white Cat Warrior creature token, then put a vigilance counter on a creature
 *       you control (chosen on resolution, not targeted).
 *  III — Target creature you control gains double strike until end of turn.
 */
class AjaniFellsTheGodsireScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AjaniFellsTheGodsire))
        return driver
    }

    /** Drain the stack, answering every target/choice request with [targets]. */
    fun GameTestDriver.drainChoosing(chooser: EntityId, targets: List<EntityId>) {
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

    /** Advance to the precombat main of the starting player's [nth] turn (turn `2n - 1`). */
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

    fun GameTestDriver.counters(entityId: EntityId, type: CounterType): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("I exiles a power-3+ opposing creature, II makes a Cat Warrior and a vigilance counter, III grants double strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 2/1
        val bears = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.WHITE, 5)
        val saga = driver.putCardInHand(controller, "Ajani Fells the Godsire")
        driver.castSpell(controller, saga)

        // Resolve the Saga spell until chapter I asks for its target.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard++ < 20) {
            if (driver.state.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
        val chapterOne = driver.state.pendingDecision as ChooseTargetsDecision
        withClue("only an opponent's creature with power 3 or greater is a legal chapter I target") {
            chapterOne.legalTargets[0]!! shouldContain courser
            chapterOne.legalTargets[0]!! shouldNotContain lions
            chapterOne.legalTargets[0]!! shouldNotContain bears
        }
        driver.drainChoosing(controller, listOf(courser))

        withClue("chapter I exiles Centaur Courser") {
            driver.findPermanent(opponent, "Centaur Courser") shouldBe null
            driver.findPermanent(opponent, "Savannah Lions") shouldNotBe null
        }

        // Chapter II: token first, then a resolution-time choice of a creature we control.
        driver.advanceToMain(2)
        driver.drainChoosing(controller, listOf(bears))

        val catWarrior = driver.getPermanents(controller).firstOrNull { driver.getCardName(it) == "Cat Warrior Token" }
        withClue("chapter II creates a 2/1 white Cat Warrior token") {
            catWarrior shouldNotBe null
            driver.state.projectedState.getPower(catWarrior!!) shouldBe 2
            driver.state.projectedState.getToughness(catWarrior) shouldBe 1
        }
        withClue("the chosen creature gets a vigilance counter and so has vigilance") {
            driver.counters(bears, CounterType.VIGILANCE) shouldBe 1
            driver.counters(catWarrior!!, CounterType.VIGILANCE) shouldBe 0
            driver.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }

        // Chapter III: double strike on target creature we control, then the Saga is sacrificed.
        driver.advanceToMain(3)
        driver.drainChoosing(controller, listOf(catWarrior!!))

        withClue("chapter III grants double strike to the target until end of turn") {
            driver.state.projectedState.hasKeyword(catWarrior, Keyword.DOUBLE_STRIKE) shouldBe true
            driver.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
        }
        withClue("the Saga is sacrificed after chapter III") {
            driver.getGraveyardCardNames(controller).contains("Ajani Fells the Godsire") shouldBe true
        }
    }

    test("chapter II can put the vigilance counter on the token it just created") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.WHITE, 5)
        val saga = driver.putCardInHand(controller, "Ajani Fells the Godsire")
        driver.castSpell(controller, saga)
        // Chapter I has no legal target (no opposing creatures), so it is removed from the stack.
        driver.drainChoosing(controller, emptyList())

        driver.advanceToMain(2)
        // No other creatures: the Cat Warrior is the only choice, so it must get the counter.
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) && guard++ < 60) {
            val decision = driver.state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> driver.submitTargetSelection(controller, listOf(decision.legalTargets[0]!!.single()))
                decision != null -> driver.autoResolveDecision()
                else -> driver.bothPass()
            }
        }

        val catWarrior = driver.getPermanents(controller).first { driver.getCardName(it) == "Cat Warrior Token" }
        driver.counters(catWarrior, CounterType.VIGILANCE) shouldBe 1
        driver.state.projectedState.hasKeyword(catWarrior, Keyword.VIGILANCE) shouldBe true
    }
})
