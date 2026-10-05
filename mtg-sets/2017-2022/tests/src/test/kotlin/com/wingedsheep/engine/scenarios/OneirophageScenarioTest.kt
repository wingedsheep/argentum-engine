package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.Oneirophage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oneirophage (MH1 #60) — "Flying. Whenever you draw a card, put a +1/+1 counter on this
 * creature."
 *
 * The trigger is per card drawn, so a three-card draw is three counters; and it is "you" draw,
 * so an opponent's draw must leave it untouched.
 */
class OneirophageScenarioTest : FunSpec({

    val drawThree = card("Test Draw Three") {
        manaCost = "{2}{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(3) }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Oneirophage)
        d.registerCard(drawThree)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    test("drawing three cards puts three +1/+1 counters on it") {
        val d = driver()
        val me = d.activePlayer!!

        val squid = d.putCreatureOnBattlefield(me, "Oneirophage")
        val spell = d.putCardInHand(me, "Test Draw Three")
        d.giveMana(me, Color.BLUE, 3)

        d.castSpell(me, spell).outcome shouldBe Outcome.Done
        d.drainStack()

        withClue("one trigger per card drawn") {
            d.plusOnes(squid) shouldBe 3
        }
        d.state.projectedState.getPower(squid) shouldBe 4
        d.state.projectedState.getToughness(squid) shouldBe 5
    }

    test("an opponent's draw doesn't trigger it, but its controller's draw step does") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)

        val squid = d.putCreatureOnBattlefield(me, "Oneirophage")
        val opponentHand = d.getHandSize(opponent)

        // Opponent's turn: they draw in their draw step.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe opponent
        d.getHandSize(opponent) shouldBe opponentHand + 1
        d.drainStack()

        withClue("only \"you\" drawing triggers it") {
            d.plusOnes(squid) shouldBe 0
        }

        // Back to my turn: my draw step draw does trigger it.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe me
        d.drainStack()

        d.plusOnes(squid) shouldBe 1
    }
})
