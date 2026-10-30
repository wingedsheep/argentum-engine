package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thassa's Intervention (THB #72) — {X}{U}{U} Instant, choose one:
 * look at the top X cards, put up to two into your hand and the rest on the bottom in a random
 * order; or counter target spell unless its controller pays twice {X}.
 *
 * Pins the counter mode's doubled X: with X = 1, one floating mana (enough for {X}) can't pay, and
 * two can.
 */
class ThassasInterventionScenarioTest : FunSpec({
    val digMode = 0
    val counterMode = 1

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (mode in listOf("cannot pay twice X", "pay", "decline")) {
        test("$mode: counters target spell unless its controller pays twice X") {
            val d = driver()
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
            d.passPriority(d.player1).error shouldBe null
            // X = 1: one mana would cover {X} but not twice {X}.
            d.giveMana(d.player1, Color.WHITE, if (mode == "cannot pay twice X") 1 else 2)

            val intervention = d.putCardInHand(d.player2, "Thassa's Intervention")
            d.giveMana(d.player2, Color.BLUE, 3)
            val target = listOf(ChosenTarget.Spell(bolt))
            d.submit(
                CastSpell(
                    d.player2, intervention, targets = target,
                    xValue = 1,
                    chosenModes = listOf(counterMode),
                    modeTargetsOrdered = listOf(target),
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            d.bothPass().error shouldBe null

            if (mode == "cannot pay twice X") {
                withClue("twice {X} is unaffordable, so no payment is offered") {
                    d.pendingDecision shouldBe null
                }
            } else {
                d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
                d.submitYesNo(d.player1, mode == "pay").error shouldBe null
            }

            val countered = mode != "pay"
            (bolt in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe countered
            (bolt in d.state.stack) shouldBe !countered
            (intervention in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
        }
    }

    test("dig mode: looks at the top X, puts up to two into hand and the rest on the bottom") {
        val d = driver()
        val third = d.putCardOnTopOfLibrary(d.player1, "Island")
        val second = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")
        val first = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val intervention = d.putCardInHand(d.player1, "Thassa's Intervention")
        d.giveMana(d.player1, Color.BLUE, 5)

        d.submit(
            CastSpell(
                d.player1, intervention, targets = emptyList(),
                xValue = 3,
                chosenModes = listOf(digMode),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        d.bothPass().error shouldBe null

        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.playerId shouldBe d.player1
        choice.options shouldContainExactlyInAnyOrder listOf(first, second, third)
        d.submitCardSelection(d.player1, listOf(first, second)).error shouldBe null

        val hand = d.getHand(d.player1)
        (first in hand) shouldBe true
        (second in hand) shouldBe true
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).last() shouldBe third
    }
})
