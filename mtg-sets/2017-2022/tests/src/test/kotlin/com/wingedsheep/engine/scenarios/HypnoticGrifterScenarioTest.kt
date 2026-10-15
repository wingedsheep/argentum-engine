package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.HypnoticGrifter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hypnotic Grifter (SNC #45) — {U} Creature — Human Rogue 1/2.
 *   {3}: This creature connives.
 *
 * Proves the activated connive is repeatable and needs no tap: each activation draws, discards, and
 * grows Hypnotic Grifter only when the discarded card is nonland.
 */
class HypnoticGrifterScenarioTest : FunSpec({

    test("each {3} activation connives; only a nonland discard adds a +1/+1 counter") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(HypnoticGrifter)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), skipMulligans = true)
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grifter = d.putCreatureOnBattlefield(you, "Hypnotic Grifter")
        val nonland = d.putCardInHand(you, "Grizzly Bears")
        val land = d.putCardInHand(you, "Forest")
        repeat(6) { d.putLandOnBattlefield(you, "Island") }

        fun counters() =
            d.state.getEntity(grifter)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun activateAndDiscard(card: EntityId) {
            d.submit(
                ActivateAbility(
                    you, grifter, HypnoticGrifter.activatedAbilities.single().id,
                    paymentStrategy = PaymentStrategy.AutoPay,
                ),
            ).outcome shouldBe Outcome.Done
            d.bothPass() // resolve: draw, then pause on the discard
            d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val decision = d.pendingDecision as SelectCardsDecision
            d.submitDecision(you, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(card)))
            d.getGraveyard(you).contains(card) shouldBe true
        }

        activateAndDiscard(land)
        counters() shouldBe 0

        activateAndDiscard(nonland)
        counters() shouldBe 1
    }
})
