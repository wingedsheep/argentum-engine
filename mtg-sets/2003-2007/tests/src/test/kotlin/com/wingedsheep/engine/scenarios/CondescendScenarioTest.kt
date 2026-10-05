package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.Condescend
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CondescendScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Condescend))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (mode in listOf("cannot pay", "pay", "decline")) {
        test("$mode: counters unless X is paid, and scry 2 happens either way") {
            val d = driver()
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
            d.passPriority(d.player1).error shouldBe null
            if (mode != "cannot pay") d.giveMana(d.player1, Color.WHITE, 2)

            val second = d.putCardOnTopOfLibrary(d.player2, "Plains")
            val first = d.putCardOnTopOfLibrary(d.player2, "Lightning Bolt")
            val condescend = d.putCardInHand(d.player2, "Condescend")
            d.giveMana(d.player2, Color.BLUE, 3)
            d.submit(
                CastSpell(
                    d.player2, condescend, targets = listOf(ChosenTarget.Spell(bolt)),
                    xValue = 2, paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            d.bothPass().error shouldBe null

            if (mode != "cannot pay") {
                d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
                d.submitYesNo(d.player1, mode == "pay").error shouldBe null
            }

            // Scry 2: Condescend's controller sees the top two cards and bottoms one.
            val scry = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            scry.playerId shouldBe d.player2
            scry.options shouldContainExactlyInAnyOrder listOf(first, second)
            d.submitCardSelection(d.player2, listOf(first)).error shouldBe null
            val order = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
            order.cards shouldBe listOf(second)
            d.submitDecision(d.player2, OrderedResponse(order.id, listOf(second))).error shouldBe null
            d.pendingDecision shouldBe null

            val library = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY))
            library.first() shouldBe second
            library.last() shouldBe first

            val countered = mode != "pay"
            (bolt in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe countered
            (bolt in d.state.stack) shouldBe !countered
            (condescend in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
        }
    }
})
