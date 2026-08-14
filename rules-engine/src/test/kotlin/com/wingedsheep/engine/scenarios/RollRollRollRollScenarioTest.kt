package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.RollRollRollRoll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chapter test for Roll-Roll-Roll-Roll (HOB #54) — {2}{U} Enchantment — Saga.
 *
 * I, II, III, IV — Exile up to one target creature or land you control. If you do, return it to the
 * battlefield under its owner's control at the beginning of the next end step.
 *
 * Covered: chapter I blinks the chosen permanent and it is back by the next turn (so chapter II can
 * pick it again), and the "up to one" is genuinely optional — declining exiles nothing.
 */
class RollRollRollRollScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RollRollRollRoll)
        return driver
    }

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

    /** Advance to the precombat main of the starting player's [nth] turn (a duel: turn 2n − 1). */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn precombat main")
    }

    fun GameTestDriver.castSaga(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.BLUE, 3)
        castSpell(controller, putCardInHand(controller, "Roll-Roll-Roll-Roll"))
    }

    test("chapter I blinks the chosen creature, which is back before chapter II") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.castSaga(controller)
        driver.drainTargeting(controller, listOf(bears))

        withClue("chapter I exiled the Bears; the returning card is a new object, so the old id is gone") {
            driver.findPermanent(controller, "Grizzly Bears") shouldBe null
            driver.getExileCardNames(controller).contains("Grizzly Bears") shouldBe true
        }

        driver.advanceToMain(2)

        withClue("the delayed trigger returned it at the beginning of the next end step") {
            driver.getPermanents(controller).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
            driver.getExileCardNames(controller).contains("Grizzly Bears") shouldBe false
        }
    }

    test("declining the 'up to one' target exiles nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.castSaga(controller)

        // Resolve the Saga spell itself, then answer chapter I's target request with nothing.
        var guard = 0
        var declined = false
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) && guard < 60) {
            val decision = driver.state.pendingDecision
            when {
                decision is ChooseTargetsDecision && !declined -> {
                    withClue("'up to one target' must allow choosing zero") {
                        decision.targetRequirements.first().minTargets shouldBe 0
                    }
                    declined = true
                    driver.submitTargetSelection(controller, emptyList())
                }
                decision != null -> driver.autoResolveDecision()
                else -> driver.bothPass()
            }
            guard++
        }

        withClue("nothing was exiled") {
            declined shouldBe true
            driver.findPermanent(controller, "Grizzly Bears") shouldBe bears
            driver.getExileCardNames(controller).isEmpty() shouldBe true
        }
    }
})
