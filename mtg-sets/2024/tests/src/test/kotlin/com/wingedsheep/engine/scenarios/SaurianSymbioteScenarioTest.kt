package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.SaurianSymbiote
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Saurian Symbiote (J25 #23) — "Reach. When this creature enters, choose one — • Put a +1/+1 counter
 * on this creature. • Create a 1/1 green Saproling creature token."
 */
class SaurianSymbioteScenarioTest : FunSpec({

    /** Casts the Symbiote, picks [mode] for its enters trigger, and drains the stack. */
    fun castChoosing(mode: Int): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SaurianSymbiote)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInHand(d.player1, "Saurian Symbiote")
        d.giveMana(d.player1, Color.GREEN, 4)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done

        var chosen = false
        var safety = 0
        while ((d.state.stack.isNotEmpty() || d.pendingDecision != null) && safety++ < 10) {
            val decision = d.pendingDecision
            if (decision is ChooseOptionDecision) {
                d.submitDecision(d.player1, OptionChosenResponse(decision.id, mode))
                chosen = true
            } else {
                d.bothPass()
            }
        }
        chosen shouldBe true
        return d to card
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("the counter mode makes it a 3/4") {
        val (d, symbiote) = castChoosing(0)
        d.plusOnes(symbiote) shouldBe 1
        d.state.projectedState.getPower(symbiote) shouldBe 3
        d.findPermanent(d.player1, "Saproling Token") shouldBe null
    }

    test("the token mode makes a Saproling and no counter") {
        val (d, symbiote) = castChoosing(1)
        d.plusOnes(symbiote) shouldBe 0
        (d.findPermanent(d.player1, "Saproling Token") != null) shouldBe true
    }
})
