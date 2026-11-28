package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.FrightfulDelusion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Frightful Delusion — "Counter target spell unless its controller pays {1}. That player discards
 * a card." The discard falls on the targeted spell's controller whether or not they paid, including
 * after the spell has been countered into the graveyard.
 */
class FrightfulDelusionScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(FrightfulDelusion))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (mode in listOf("cannot pay", "pay", "decline")) {
        test("$mode: counters unless {1} is paid, and the spell's controller discards either way") {
            val d = driver()
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
            d.passPriority(d.player1).error shouldBe null
            if (mode != "cannot pay") d.giveMana(d.player1, Color.WHITE, 1)

            // At least two cards in hand so the discard is a real choice.
            val keep = d.putCardInHand(d.player1, "Plains")
            val toss = d.putCardInHand(d.player1, "Lightning Bolt")
            val delusion = d.putCardInHand(d.player2, "Frightful Delusion")
            d.giveMana(d.player2, Color.BLUE, 3)
            d.submit(
                CastSpell(
                    d.player2, delusion, targets = listOf(ChosenTarget.Spell(bolt)),
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            d.bothPass().error shouldBe null

            if (mode != "cannot pay") {
                d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
                d.submitYesNo(d.player1, mode == "pay").error shouldBe null
            }

            val discard = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            discard.playerId shouldBe d.player1
            discard.options shouldContainExactlyInAnyOrder d.state.getZone(ZoneKey(d.player1, Zone.HAND))
            val handBefore = d.state.getZone(ZoneKey(d.player1, Zone.HAND))
            d.submitCardSelection(d.player1, listOf(toss)).error shouldBe null
            d.pendingDecision shouldBe null

            d.state.getZone(ZoneKey(d.player1, Zone.HAND)) shouldContainExactlyInAnyOrder handBefore - toss
            (keep in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe true
            (toss in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true

            val countered = mode != "pay"
            (bolt in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe countered
            (bolt in d.state.stack) shouldBe !countered
            (delusion in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
        }
    }
})
