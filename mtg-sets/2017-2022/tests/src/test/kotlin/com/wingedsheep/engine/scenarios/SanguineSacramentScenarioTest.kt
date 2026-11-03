package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.SanguineSacrament
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sanguine Sacrament (XLN #33): "You gain twice X life. Put Sanguine Sacrament on the bottom of its
 * owner's library." — one life-gain event of 2X, then the card goes to the bottom of the library
 * (unshuffled) instead of the graveyard.
 */
class SanguineSacramentScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(SanguineSacrament))
        it.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("X=3 gains 6 life in one event and the card goes to the bottom of its owner's library") {
        val d = driver()
        val me = d.player1
        val libraryBefore = d.state.getZone(ZoneKey(me, Zone.LIBRARY))
        val shufflesBefore = d.events.count { it is LibraryShuffledEvent }
        val lifeEventsBefore = d.events.count { it is LifeChangedEvent }

        val spell = d.putCardInHand(me, "Sanguine Sacrament")
        d.giveMana(me, Color.WHITE, 5)
        d.submit(CastSpell(me, spell, xValue = 3, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.bothPass()

        d.getLifeTotal(me) shouldBe 26
        d.events.count { it is LifeChangedEvent } shouldBe lifeEventsBefore + 1
        d.state.getZone(ZoneKey(me, Zone.LIBRARY)) shouldBe libraryBefore + spell
        d.getGraveyardCardNames(me).contains("Sanguine Sacrament") shouldBe false
        d.events.count { it is LibraryShuffledEvent } shouldBe shufflesBefore
    }
})
