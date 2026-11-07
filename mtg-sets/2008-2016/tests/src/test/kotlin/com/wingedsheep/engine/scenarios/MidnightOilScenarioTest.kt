package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.kld.cards.MidnightOil
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Midnight Oil (KLD #92) — "This enchantment enters with seven hour counters on it. At the
 * beginning of your draw step, draw an additional card and remove two hour counters from this
 * enchantment. Your maximum hand size is equal to the number of hour counters on this enchantment.
 * Whenever you discard a card, you lose 1 life."
 */
class MidnightOilScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MidnightOil)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.hourCounters(oil: EntityId): Int =
        state.getEntity(oil)?.get<CountersComponent>()?.getCount(CounterType.HOUR) ?: 0

    fun GameTestDriver.castOil(): EntityId {
        giveMana(player1, Color.BLACK, 4)
        val oil = putCardInHand(player1, "Midnight Oil")
        val cast = castSpell(player1, oil)
        withClue(cast.error ?: "casting Midnight Oil failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return oil
    }

    test("enters with seven hour counters, and the cleanup discard costs life in that cleanup step") {
        val d = driver()
        val oil = d.castOil()
        d.hourCounters(oil) shouldBe 7
        val turn = d.state.turnNumber
        repeat(2) { d.putCardInHand(d.player1, "Swamp") }

        d.passPriorityUntil(Step.END)
        d.bothPass()
        val discard = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discard.minSelections shouldBe 2
        d.submitCardSelection(d.player1, d.getHand(d.player1).take(2))

        withClue("the life-loss triggers go on the stack before the turn ends (CR 514.3a)") {
            d.state.turnNumber shouldBe turn
            d.state.step shouldBe Step.CLEANUP
            d.assertStackSize(2)
        }
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 18
        d.bothPass()
        d.activePlayer shouldBe d.player2
    }

    test("the draw step draws an extra card and removes two hour counters, shrinking the hand size") {
        val d = driver()
        val oil = d.castOil()

        d.passPriorityUntil(Step.DRAW, d.player1)
        val handAfterNormalDraw = d.getHand(d.player1).size
        d.bothPass()
        d.getHand(d.player1).size shouldBe handAfterNormalDraw + 1
        d.hourCounters(oil) shouldBe 5

        // The maximum hand size is now the five remaining hour counters.
        d.passPriorityUntil(Step.END)
        d.bothPass()
        val discard = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discard.minSelections shouldBe d.getHand(d.player1).size - 5
    }
})
