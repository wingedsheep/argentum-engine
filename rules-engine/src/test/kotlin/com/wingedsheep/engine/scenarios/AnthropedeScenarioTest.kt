package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Anthropede (DSK #167) — {3}{G} Creature — Insect 3/4, Reach.
 *
 * "When this creature enters, you may discard a card or pay {2}. When you do, destroy target Room."
 *
 * The enters trigger is an optional `ReflexiveTriggerEffect`: the action is a two-way
 * `ChooseActionEffect` (discard a card / pay {2}); taking it queues a reflexive trigger that
 * targets and destroys a Room. Exercises the may-decline path, the discard branch, and that the
 * reflexive trigger actually destroys the chosen Room.
 */
class AnthropedeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Resolve the cast spell + ETB reflexive trigger until we hit the optional "you may" decision.
    fun GameTestDriver.advanceToMayDecision(p1: EntityId) {
        var guard = 0
        while (guard++ < 20) {
            if (pendingDecision is YesNoDecision) return
            if (state.stack.isNotEmpty() || state.priorityPlayerId != null) bothPass() else break
        }
    }

    test("discard a card to destroy a target Room") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.giveMana(p1, Color.GREEN, 4)

        // A Room belonging to the opponent to be destroyed.
        val room = driver.putPermanentOnBattlefield(driver.player2, "Unholy Annex // Ritual Chamber")
        // A spare hand card to discard for the cost.
        driver.putCardInHand(p1, "Forest")

        val anthropede = driver.putCardInHand(p1, "Anthropede")
        driver.castSpell(p1, anthropede)
        driver.advanceToMayDecision(p1)

        // The optional "you may discard a card or pay {2}" decision.
        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(p1, true)

        // Choose the "Discard a card" option.
        val choose = driver.pendingDecision as ChooseOptionDecision
        val discardIdx = choose.options.indexOfFirst { it.contains("Discard", ignoreCase = true) }
        driver.submitDecision(p1, OptionChosenResponse(choose.id, discardIdx))

        // Walk follow-up decisions: discard selection, then the reflexive Room target.
        var guard = 0
        while (guard++ < 12) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(p1, listOf(room))
                is SelectCardsDecision ->
                    driver.submitCardSelection(p1, listOf(driver.state.getZone(p1, Zone.HAND).first()))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        // The Room is destroyed — no longer on the battlefield.
        driver.state.getZone(driver.player2, Zone.BATTLEFIELD).contains(room) shouldBe false
        driver.findPermanent(driver.player2, "Unholy Annex // Ritual Chamber") shouldBe null
    }

    test("paying {2} also lets you destroy a target Room") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.giveMana(p1, Color.GREEN, 4)   // for casting {3}{G}
        driver.giveColorlessMana(p1, 2)        // for the "pay {2}" option

        val room = driver.putPermanentOnBattlefield(driver.player2, "Unholy Annex // Ritual Chamber")

        val anthropede = driver.putCardInHand(p1, "Anthropede")
        driver.castSpell(p1, anthropede)
        driver.advanceToMayDecision(p1)

        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(p1, true)

        val choose = driver.pendingDecision as ChooseOptionDecision
        val payIdx = choose.options.indexOfFirst { it.contains("Pay", ignoreCase = true) }
        driver.submitDecision(p1, OptionChosenResponse(choose.id, payIdx))

        var guard = 0
        while (guard++ < 12) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(p1, listOf(room))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        driver.findPermanent(driver.player2, "Unholy Annex // Ritual Chamber") shouldBe null
    }

    test("declining the may leaves the Room intact") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.giveMana(p1, Color.GREEN, 4)

        driver.putPermanentOnBattlefield(driver.player2, "Unholy Annex // Ritual Chamber")

        val anthropede = driver.putCardInHand(p1, "Anthropede")
        driver.castSpell(p1, anthropede)
        driver.advanceToMayDecision(p1)

        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(p1, false)

        var guard = 0
        while (guard++ < 8 && driver.state.stack.isNotEmpty()) driver.bothPass()

        // The Room survives.
        driver.findPermanent(driver.player2, "Unholy Annex // Ritual Chamber") shouldNotBe null
    }
})
