package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kicker "enters with …" riders are replacement effects (CR 614.1c), not ETB triggers:
 * nothing goes on the stack, and the counters/keywords are present from the moment the
 * permanent enters. Exercised through Kavu Titan ({1}{G} 2/2, Kicker {2}{G} — "If this
 * creature was kicked, it enters with three +1/+1 counters on it and with trample") and
 * Pouncing Kavu ({1}{R} 1/1 first strike, Kicker {2}{R} — "… enters with two +1/+1
 * counters on it and with haste").
 */
class KickedEntersWithReplacementScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castKicked(playerId: EntityId, cardId: EntityId) =
        submit(
            CastSpell(
                playerId = playerId,
                cardId = cardId,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

    test("kicked Kavu Titan enters as a 5/5 with trample, with no trigger on the stack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        driver.giveMana(you, Color.GREEN, 6)
        val titan = driver.putCardInHand(you, "Kavu Titan")
        driver.castKicked(you, titan).error shouldBe null
        driver.bothPass() // resolve onto the battlefield

        // Replacement effect: counters and trample are there immediately, nothing triggered.
        driver.stackSize shouldBe 0
        val perm = driver.findPermanent(you, "Kavu Titan")!!
        driver.plusOneCounters(perm) shouldBe 3
        driver.state.projectedState.getPower(perm) shouldBe 5
        driver.state.projectedState.getToughness(perm) shouldBe 5
        driver.state.projectedState.hasKeyword(perm, Keyword.TRAMPLE) shouldBe true
    }

    test("unkicked Kavu Titan enters as a plain 2/2 without trample") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        driver.giveMana(you, Color.GREEN, 2)
        val titan = driver.putCardInHand(you, "Kavu Titan")
        driver.castSpell(you, titan).error shouldBe null
        driver.bothPass()

        driver.stackSize shouldBe 0
        val perm = driver.findPermanent(you, "Kavu Titan")!!
        driver.plusOneCounters(perm) shouldBe 0
        driver.state.projectedState.getPower(perm) shouldBe 2
        driver.state.projectedState.getToughness(perm) shouldBe 2
        driver.state.projectedState.hasKeyword(perm, Keyword.TRAMPLE) shouldBe false
    }

    test("kicked Pouncing Kavu has haste from the moment it enters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        driver.giveMana(you, Color.RED, 5) // {1}{R} base + {2}{R} kicker
        val kavu = driver.putCardInHand(you, "Pouncing Kavu")
        driver.castKicked(you, kavu).error shouldBe null
        driver.bothPass()

        driver.stackSize shouldBe 0
        val perm = driver.findPermanent(you, "Pouncing Kavu")!!
        driver.plusOneCounters(perm) shouldBe 2
        driver.state.projectedState.hasKeyword(perm, Keyword.HASTE) shouldBe true
        // The printed keyword is untouched by the entry grant.
        driver.state.projectedState.hasKeyword(perm, Keyword.FIRST_STRIKE) shouldBe true

        // Haste is real: it can attack the turn it entered.
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(perm), defendingPlayer = opponent).error shouldBe null
    }

    test("bounced and recast without kicker: enters as a fresh 2/2 (CR 400.7)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        driver.giveMana(you, Color.GREEN, 6)
        val titan = driver.putCardInHand(you, "Kavu Titan")
        driver.castKicked(you, titan).error shouldBe null
        driver.bothPass()
        val kicked = driver.findPermanent(you, "Kavu Titan")!!
        driver.state.projectedState.hasKeyword(kicked, Keyword.TRAMPLE) shouldBe true

        // Bounce it: the new object has no memory of being kicked (CR 400.7) —
        // the keyword grant and counters must not survive the round trip.
        driver.giveMana(you, Color.BLUE, 1)
        val unsummon = driver.putCardInHand(you, "Unsummon")
        driver.castSpell(you, unsummon, targets = listOf(kicked)).error shouldBe null
        driver.bothPass()
        driver.findPermanent(you, "Kavu Titan") shouldBe null

        driver.giveMana(you, Color.GREEN, 2)
        val again = driver.findCardInHand(you, "Kavu Titan")!!
        driver.castSpell(you, again).error shouldBe null
        driver.bothPass()

        val perm = driver.findPermanent(you, "Kavu Titan")!!
        driver.plusOneCounters(perm) shouldBe 0
        driver.state.projectedState.getPower(perm) shouldBe 2
        driver.state.projectedState.hasKeyword(perm, Keyword.TRAMPLE) shouldBe false
    }
})
