package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ScurrilousSentry
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scurrilous Sentry (MH3 #108) — {3}{B} Creature — Human Knight Rogue 2/3.
 *   Menace
 *   Whenever this creature enters or attacks, it connives.
 *
 * Both halves of "enters or attacks" connive on the Sentry itself: a nonland discard grows it,
 * a land discard does not.
 */
class ScurrilousSentryScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(ScurrilousSentry)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), skipMulligans = true)
        return d
    }

    fun GameTestDriver.discard(card: EntityId) {
        isPaused shouldBe true
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitDecision(player1, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(card)))
        isPaused shouldBe false
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("entering connives; discarding a nonland puts a +1/+1 counter on the Sentry") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val nonland = d.putCardInHand(you, "Grizzly Bears")
        val sentry = d.putCardInHand(you, "Scurrilous Sentry")
        repeat(4) { d.putLandOnBattlefield(you, "Swamp") }

        d.submit(CastSpell(playerId = you, cardId = sentry, paymentStrategy = PaymentStrategy.AutoPay))
            .outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the ETB connive trigger

        d.discard(nonland)
        d.getGraveyard(you).contains(nonland) shouldBe true
        d.plusCounters(sentry) shouldBe 1
    }

    test("attacking connives; discarding a land adds no counter") {
        val d = driver()
        val you = d.player1
        val sentry = d.putCreatureOnBattlefield(you, "Scurrilous Sentry")
        d.removeSummoningSickness(sentry)
        val land = d.putCardInHand(you, "Forest")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(sentry), d.player2)
        val handBefore = d.getHandSize(you)
        d.bothPass() // resolve the attack connive trigger

        d.getHandSize(you) shouldBe handBefore + 1 // connive draws before it discards
        d.discard(land)
        d.getGraveyard(you).contains(land) shouldBe true
        d.plusCounters(sentry) shouldBe 0
    }

    test("attacking connives; discarding a nonland grows the Sentry") {
        val d = driver()
        val you = d.player1
        val sentry = d.putCreatureOnBattlefield(you, "Scurrilous Sentry")
        d.removeSummoningSickness(sentry)
        val nonland = d.putCardInHand(you, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(sentry), d.player2)
        d.bothPass()

        d.discard(nonland)
        d.plusCounters(sentry) shouldBe 1
    }
})
