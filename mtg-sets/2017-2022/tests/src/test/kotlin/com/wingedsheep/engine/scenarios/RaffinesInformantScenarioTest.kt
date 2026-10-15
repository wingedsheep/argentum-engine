package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.RaffinesInformant
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Raffine's Informant (SNC #26) — {1}{W} Creature — Human Wizard 2/1.
 *   When this creature enters, it connives.
 *
 * Proves the ETB connive lands its +1/+1 counter on Raffine's Informant itself: discarding a nonland grows it,
 * discarding a land does not.
 */
class RaffinesInformantScenarioTest : FunSpec({

    /** Cast Raffine's Informant, resolve it and its ETB trigger, and pause on the connive discard. */
    fun enterAndConnive(discardName: String): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(RaffinesInformant)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), skipMulligans = true)
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val toDiscard = d.putCardInHand(you, discardName)
        val creature = d.putCardInHand(you, "Raffine's Informant")
        d.putLandOnBattlefield(you, "Plains")
        d.putLandOnBattlefield(you, "Island")

        d.submit(
            CastSpell(playerId = you, cardId = creature, paymentStrategy = PaymentStrategy.AutoPay),
        ).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature spell; the ETB trigger goes on the stack
        d.bothPass() // resolve the trigger: draw, then pause on the discard

        d.isPaused shouldBe true
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return Triple(d, creature, toDiscard)
    }

    fun discard(d: GameTestDriver, card: EntityId) {
        val decision = d.pendingDecision as SelectCardsDecision
        d.submitDecision(d.player1, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(card)))
        d.isPaused shouldBe false
        d.getGraveyard(d.player1).contains(card) shouldBe true
    }

    fun plusOneCounters(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("discarding a nonland card puts a +1/+1 counter on Raffine's Informant") {
        val (d, creature, nonland) = enterAndConnive("Grizzly Bears")
        discard(d, nonland)
        plusOneCounters(d, creature) shouldBe 1
    }

    test("discarding a land adds no counter") {
        val (d, creature, land) = enterAndConnive("Forest")
        discard(d, land)
        plusOneCounters(d, creature) shouldBe 0
    }
})
