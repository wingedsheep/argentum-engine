package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MindsEye
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mind's Eye (MRD #205) — {5} Artifact.
 *
 * "Whenever an opponent draws a card, you may pay {1}. If you do, draw a card."
 *
 * Pins the three things that can go wrong with this shape: paying actually draws, declining draws
 * nothing, and the trigger fires per *individual* card the opponent draws (CR 121.2) rather than
 * once per draw spell.
 */
class MindsEyeScenarioTest : FunSpec({

    val drawOne = card("Test Draw One") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val drawTwo = card("Test Draw Two") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(2) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MindsEye, drawOne, drawTwo))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Resolve everything on the stack, answering each Mind's Eye payment offer with [pay], and
     * return how many offers were made. The optional-mana gate surfaces either as a plain yes/no
     * (mana already floating) or as a mana-source selection, so both are handled.
     */
    fun GameTestDriver.settle(me: EntityId, pay: Boolean): Int {
        var offers = 0
        var guard = 0
        while ((stackSize > 0 || isPaused) && guard++ < 40) {
            when (pendingDecision) {
                is YesNoDecision -> {
                    submitYesNo(me, pay); offers++
                }
                is SelectManaSourcesDecision -> {
                    submitManaAutoPayOrDecline(me, autoPay = pay); offers++
                }
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
        return offers
    }

    fun GameTestDriver.opponentCasts(spellName: String, opponent: EntityId) {
        val spell = putCardInHand(opponent, spellName)
        giveMana(opponent, Color.BLUE, 1)
        castSpell(opponent, spell)
    }

    test("paying {1} on an opponent's draw draws a card") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putPermanentOnBattlefield(me, "Mind's Eye")
        driver.giveColorlessMana(me, 1)
        val handBefore = driver.getHandSize(me)

        driver.passPriority(me)
        driver.opponentCasts("Test Draw One", opponent)

        driver.settle(me, pay = true) shouldBe 1
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("declining the payment draws nothing") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putPermanentOnBattlefield(me, "Mind's Eye")
        driver.giveColorlessMana(me, 1)
        val handBefore = driver.getHandSize(me)

        driver.passPriority(me)
        driver.opponentCasts("Test Draw One", opponent)

        driver.settle(me, pay = false) shouldBe 1
        driver.getHandSize(me) shouldBe handBefore
    }

    test("an opponent drawing two cards triggers twice, once per card") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putPermanentOnBattlefield(me, "Mind's Eye")
        driver.giveColorlessMana(me, 2)
        val handBefore = driver.getHandSize(me)

        driver.passPriority(me)
        driver.opponentCasts("Test Draw Two", opponent)

        // Two separate trigger instances, each with its own payment offer.
        driver.settle(me, pay = true) shouldBe 2
        driver.getHandSize(me) shouldBe handBefore + 2
    }

    test("your own draws never trigger it") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        driver.putPermanentOnBattlefield(me, "Mind's Eye")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.BLUE, 1)
        val handBefore = driver.getHandSize(me)

        val spell = driver.putCardInHand(me, "Test Draw One")
        driver.castSpell(me, spell)

        // No payment offer at all. Net hand change: +1 (the spell was added), -1 (it was cast),
        // +1 (its own draw) = handBefore + 1 — the extra Mind's Eye card is absent.
        driver.settle(me, pay = true) shouldBe 0
        driver.getHandSize(me) shouldBe handBefore + 1
    }
})
