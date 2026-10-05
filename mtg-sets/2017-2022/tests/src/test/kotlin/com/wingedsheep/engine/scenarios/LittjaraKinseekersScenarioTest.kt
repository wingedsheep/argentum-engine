package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.LittjaraKinseekers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Littjara Kinseekers — {3}{U} Creature — Shapeshifter 2/4 (KHM; reprinted in J22).
 *
 *   Changeling
 *   When this creature enters, if you control three or more creatures that share a creature type,
 *   put a +1/+1 counter on this creature, then scry 1.
 */
class LittjaraKinseekersScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LittjaraKinseekers))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castKinseekers(me: EntityId): EntityId {
        val card = putCardInHand(me, "Littjara Kinseekers")
        giveMana(me, Color.BLUE, 4)
        submit(CastSpell(me, card, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        bothPass() // the creature spell resolves
        return card
    }

    test("with two other Warriors (changeling makes three) it gets a +1/+1 counter, then scries 1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Centaur Courser") // Centaur Warrior
        driver.putPermanentOnBattlefield(me, "Phantom Warrior") // Illusion Warrior
        val top = driver.putCardOnTopOfLibrary(me, "Forest")

        driver.castKinseekers(me)
        driver.state.stack.size shouldBe 1
        driver.bothPass() // the enters trigger resolves

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(top)
        driver.submitCardSelection(me, listOf(top))
        driver.state.getLibrary(me).last() shouldBe top

        val kinseekers = driver.findPermanent(me, "Littjara Kinseekers").shouldNotBeNull()
        driver.state.projectedState.getPower(kinseekers) shouldBe 3
        driver.state.projectedState.getToughness(kinseekers) shouldBe 5
    }

    test("creatures that each share a type only with the changeling don't count as three") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Savannah Lions") // Cat
        driver.putPermanentOnBattlefield(me, "Goblin Guide") // Goblin Scout

        driver.castKinseekers(me)
        driver.state.stack.size shouldBe 0
        driver.state.pendingDecision.shouldBeNull()

        val kinseekers = driver.findPermanent(me, "Littjara Kinseekers").shouldNotBeNull()
        driver.state.projectedState.getPower(kinseekers) shouldBe 2
        driver.state.projectedState.getToughness(kinseekers) shouldBe 4
    }

    test("losing the third shared-type creature before resolution means no counter and no scry") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Centaur Courser")
        val phantom = driver.putPermanentOnBattlefield(me, "Phantom Warrior")

        driver.castKinseekers(me)
        driver.state.stack.size shouldBe 1

        driver.moveToGraveyard(phantom)
        driver.bothPass()

        driver.state.stack.size shouldBe 0
        driver.state.pendingDecision.shouldBeNull()
        val kinseekers = driver.findPermanent(me, "Littjara Kinseekers").shouldNotBeNull()
        driver.state.projectedState.getPower(kinseekers) shouldBe 2
        driver.state.projectedState.getToughness(kinseekers) shouldBe 4
    }
})
