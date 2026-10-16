package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ToluzCleverConductor
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Toluz, Clever Conductor (SNC #228) — {W/U}{U}{U/B} Legendary Creature — Human Rogue 3/1.
 *   When Toluz enters, it connives.
 *   Whenever you discard one or more cards, exile them from your graveyard.
 *   When Toluz dies, put the cards exiled with it into their owner's hand.
 *
 * Proves the connive discard is exiled by Toluz's own batch discard trigger, and that the card
 * returns to its owner's hand when Toluz dies.
 */
class ToluzCleverConductorScenarioTest : FunSpec({

    test("the connive discard is exiled, and returns to hand when Toluz dies") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(ToluzCleverConductor)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), skipMulligans = true)
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putCardInHand(you, "Grizzly Bears")
        val toluz = d.putCardInHand(you, "Toluz, Clever Conductor")
        repeat(3) { d.putLandOnBattlefield(you, "Island") }

        d.submit(CastSpell(playerId = you, cardId = toluz, paymentStrategy = PaymentStrategy.AutoPay))
            .outcome shouldBe Outcome.Done
        d.bothPass() // resolve Toluz; the connive trigger goes on the stack
        d.bothPass() // resolve connive: draw, then pause on the discard

        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(bears)))
        d.bothPass() // resolve the discard trigger

        d.getExile(you) shouldContain bears
        d.getGraveyard(you) shouldNotContain bears

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.putLandOnBattlefield(you, "Mountain")
        d.castSpell(you, bolt, targets = listOf(toluz)).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the Bolt; Toluz dies and its trigger goes on the stack
        d.bothPass() // resolve the dies trigger
        d.getGraveyard(you) shouldContain toluz
        d.getHand(you) shouldContain bears
        d.getExile(you) shouldNotContain bears
    }
})
