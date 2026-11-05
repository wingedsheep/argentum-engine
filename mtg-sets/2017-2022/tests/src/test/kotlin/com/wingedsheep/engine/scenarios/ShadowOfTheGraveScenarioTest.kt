package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.DesertCerodon
import com.wingedsheep.mtg.sets.definitions.akh.cards.ShadowOfTheGrave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Shadow of the Grave (AKH #107) — "Return to your hand all cards in your graveyard that you cycled
 * or discarded this turn."
 *
 * Cycling is a discard, so the cycled card comes back; a card that reached the graveyard another
 * way, or was discarded on an earlier turn, stays put.
 */
class ShadowOfTheGraveScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ShadowOfTheGrave, DesertCerodon))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.cycle(cardName: String) = putCardInHand(player1, cardName).also { card ->
        giveMana(player1, Color.RED, 1)
        val result = submit(CycleCard(player1, card, paymentStrategy = PaymentStrategy.FromPool))
        withClue("cycling failed: ${result.error}") { result.outcome shouldBe Outcome.Done }
        while (state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.castShadow() {
        val shadow = putCardInHand(player1, "Shadow of the Grave")
        giveMana(player1, Color.BLACK, 2)
        submitSuccess(CastSpell(player1, shadow, paymentStrategy = PaymentStrategy.FromPool))
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("returns the card you cycled this turn and leaves other graveyard cards") {
        val d = driver()
        val cycled = d.cycle("Desert Cerodon")
        val notDiscarded = d.putCardInGraveyard(d.player1, "Centaur Courser")
        d.getGraveyard(d.player1) shouldContain cycled

        d.castShadow()

        d.getHand(d.player1) shouldContain cycled
        d.getGraveyard(d.player1) shouldContain notDiscarded
        d.getHand(d.player1) shouldNotContain notDiscarded
    }

    test("a card discarded on an earlier turn is not returned") {
        val d = driver()
        val cycled = d.cycle("Desert Cerodon")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player2)
        // The opponent passes, so player 1 casts the instant on the opponent's turn.
        d.passPriority(d.player2)

        d.castShadow()

        d.getGraveyard(d.player1) shouldContain cycled
        d.getHand(d.player1) shouldNotContain cycled
    }
})
