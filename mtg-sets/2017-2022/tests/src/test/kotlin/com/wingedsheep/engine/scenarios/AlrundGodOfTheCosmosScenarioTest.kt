package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Alrund, God of the Cosmos // Hakka, Whispering Raven (KHM #40).
 *
 *   Alrund gets +1/+1 for each card in your hand and each foretold card you own in exile.
 *   At the beginning of your end step, choose a card type, then reveal the top two cards of your
 *   library. Put all cards of the chosen type revealed this way into your hand and the rest on the
 *   bottom of your library in any order.
 *
 * Pins the resolution-time card-type choice (`OptionType.CARD_TYPE` read back by
 * `CardPredicate.CardTypeEqualsChosen`) and the hand + foretold count.
 */
class AlrundGodOfTheCosmosScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Advances to the end step and resolves Alrund's trigger up to its card-type prompt. */
    fun GameTestDriver.toCardTypeChoice(): ChooseOptionDecision {
        passPriorityUntil(Step.END)
        var guard = 0
        while (state.pendingDecision == null && state.stack.isNotEmpty() && guard++ < 10) bothPass()
        return state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("gets +1/+1 for each card in hand and each foretold card owned in exile") {
        val d = driver()
        val p = d.activePlayer!!
        val alrund = d.putCreatureOnBattlefield(p, "Alrund, God of the Cosmos")
        val hand = d.getHandSize(p)
        d.state.projectedState.getPower(alrund) shouldBe 1 + hand
        d.state.projectedState.getToughness(alrund) shouldBe 1 + hand

        val cup = d.putCardInHand(p, "Poison the Cup")
        d.giveMana(p, Color.BLACK, 2)
        d.submitSuccess(ForetellCard(p, cup))
        // The foretold card left the hand but still counts from exile.
        d.state.projectedState.getPower(alrund) shouldBe 1 + hand + 1
    }

    test("end step: cards of the chosen type go to hand, the rest to the bottom") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, "Alrund, God of the Cosmos")
        val bolt = d.putCardOnTopOfLibrary(p, "Lightning Bolt")
        val bears = d.putCardOnTopOfLibrary(p, "Grizzly Bears")

        val decision = d.toCardTypeChoice()
        decision.options shouldContain "Kindred"
        decision.options shouldNotContain "Vanguard"
        decision.options shouldNotContain "Snow"
        d.submitDecision(p, OptionChosenResponse(decision.id, decision.options.indexOf("Creature")))
            .error shouldBe null
        while (d.state.stack.isNotEmpty() && d.state.pendingDecision == null) d.bothPass()

        d.getHand(p) shouldContain bears
        d.getHand(p) shouldNotContain bolt
        d.state.getLibrary(p).last() shouldBe bolt
    }

    test("no revealed card of the chosen type: both go to the bottom in the order chosen") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, "Alrund, God of the Cosmos")
        val bolt = d.putCardOnTopOfLibrary(p, "Lightning Bolt")
        val bears = d.putCardOnTopOfLibrary(p, "Grizzly Bears")

        val decision = d.toCardTypeChoice()
        d.submitDecision(p, OptionChosenResponse(decision.id, decision.options.indexOf("Land")))
            .error shouldBe null
        val order = d.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        order.cards.toSet() shouldBe setOf(bears, bolt)
        d.submitDecision(p, OrderedResponse(order.id, listOf(bolt, bears))).error shouldBe null

        d.getHand(p) shouldNotContain bears
        d.getHand(p) shouldNotContain bolt
        d.state.getLibrary(p).takeLast(2).toSet() shouldContainExactly setOf(bolt, bears)
    }
})
