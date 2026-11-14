package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.Nightsnare
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Nightsnare (ORI #111) — {3}{B} Sorcery.
 *   Target opponent reveals their hand. You may choose a nonland card from it. If you do, that
 *   player discards that card. If you don't, that player discards two cards.
 *
 * Pins the either/or and who chooses on each side: the caster's pick discards exactly that card;
 * declining (or there being no nonland card to take) hands the choice to the opponent, who
 * discards two cards of their own choosing (2015-06-22 ruling).
 */
class NightsnareScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + Nightsnare)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun cast(d: GameTestDriver) {
        val nightsnare = d.putCardInHand(d.player1, Nightsnare.name)
        d.giveMana(d.player1, Color.BLACK, 4)
        d.submit(
            CastSpell(
                d.player1, nightsnare,
                targets = listOf(ChosenTarget.Player(d.player2)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()
    }

    test("choosing a nonland card makes that player discard exactly that card") {
        val d = setup()
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.putCardInHand(d.player2, "Grizzly Bears")
        val handBefore = d.getHandSize(d.player2)
        cast(d)

        val pick = d.pendingDecision as SelectCardsDecision
        pick.playerId shouldBe d.player1
        d.submitDecision(d.player1, CardsSelectedResponse(pick.id, listOf(bolt)))

        d.pendingDecision shouldBe null
        d.getGraveyardCardNames(d.player2) shouldContain "Lightning Bolt"
        d.getGraveyardCardNames(d.player2) shouldNotContain "Grizzly Bears"
        d.getHandSize(d.player2) shouldBe handBefore - 1
    }

    test("declining makes the opponent discard two cards of their own choosing") {
        val d = setup()
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        val bears = d.putCardInHand(d.player2, "Grizzly Bears")
        val handBefore = d.getHandSize(d.player2)
        cast(d)

        val pick = d.pendingDecision as SelectCardsDecision
        pick.playerId shouldBe d.player1
        d.submitDecision(d.player1, CardsSelectedResponse(pick.id, emptyList()))

        val discard = d.pendingDecision as SelectCardsDecision
        discard.playerId shouldBe d.player2
        discard.minSelections shouldBe 2
        d.submitDecision(d.player2, CardsSelectedResponse(discard.id, listOf(bolt, bears)))

        d.pendingDecision shouldBe null
        d.getGraveyardCardNames(d.player2) shouldContain "Lightning Bolt"
        d.getGraveyardCardNames(d.player2) shouldContain "Grizzly Bears"
        d.getHandSize(d.player2) shouldBe handBefore - 2
    }

    test("with no nonland card to take, the opponent still discards two cards") {
        val d = setup()
        val handBefore = d.getHandSize(d.player2)
        cast(d)

        // The caster may still be shown the all-land hand; nothing in it can be chosen.
        (d.pendingDecision as? SelectCardsDecision)?.takeIf { it.playerId == d.player1 }?.let {
            d.submitDecision(d.player1, CardsSelectedResponse(it.id, emptyList()))
        }

        val discard = d.pendingDecision as SelectCardsDecision
        discard.playerId shouldBe d.player2
        d.submitDecision(d.player2, CardsSelectedResponse(discard.id, discard.options.take(2)))

        d.pendingDecision shouldBe null
        d.getHandSize(d.player2) shouldBe handBefore - 2
    }
})
