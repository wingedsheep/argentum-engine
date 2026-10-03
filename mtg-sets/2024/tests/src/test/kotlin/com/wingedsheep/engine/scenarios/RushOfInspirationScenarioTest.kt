package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.RushOfInspiration
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rush of Inspiration // Crackling Falls (MH3).
 *
 * Front: "Draw two cards. Then discard a card at random unless you pay {E}{E}."
 * Back: "This land enters tapped. {T}: Add {U} or {R}."
 */
class RushOfInspirationScenarioTest : FunSpec({
    fun driverAtMain(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + RushOfInspiration)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.castRush() {
        val card = putCardInHand(player1, "Rush of Inspiration")
        giveMana(player1, Color.BLUE, 1)
        giveMana(player1, Color.RED, 2)
        castSpell(player1, card).error shouldBe null
        bothPass()
    }

    test("paying two energy keeps both drawn cards") {
        val d = driverAtMain()
        d.giveEnergy(3)
        val handBefore = d.getHandSize(d.player1)
        d.castRush()
        val question = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        question.playerId shouldBe d.player1
        d.submitDecision(d.player1, YesNoResponse(question.id, true)).error shouldBe null
        d.energy() shouldBe 1
        // +1 Rush put in hand, -1 cast, +2 drawn
        d.getHandSize(d.player1) shouldBe handBefore + 2
        d.getGraveyardCardNames(d.player1) shouldBe listOf("Rush of Inspiration")
    }

    test("declining to pay discards a card at random and keeps the energy") {
        val d = driverAtMain()
        d.giveEnergy(2)
        val handBefore = d.getHandSize(d.player1)
        d.castRush()
        val question = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitDecision(d.player1, YesNoResponse(question.id, false)).error shouldBe null
        d.pendingDecision shouldBe null
        d.energy() shouldBe 2
        d.getHandSize(d.player1) shouldBe handBefore + 1
        d.getGraveyard(d.player1).size shouldBe 2
    }

    test("with fewer than two energy the random discard simply happens") {
        val d = driverAtMain()
        d.giveEnergy(1)
        val handBefore = d.getHandSize(d.player1)
        d.castRush()
        d.pendingDecision shouldBe null
        d.energy() shouldBe 1
        d.getHandSize(d.player1) shouldBe handBefore + 1
        d.getGraveyard(d.player1).size shouldBe 2
    }

    test("Crackling Falls enters tapped") {
        val d = driverAtMain()
        val card = d.putCardInHand(d.player1, "Rush of Inspiration")
        d.submit(PlayLand(d.player1, card, asBackFace = true)).error shouldBe null
        d.state.getEntity(card)!!.has<TappedComponent>() shouldBe true
    }
})
