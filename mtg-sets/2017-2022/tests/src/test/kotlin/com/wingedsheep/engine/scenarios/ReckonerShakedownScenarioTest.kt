package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.ReckonerShakedown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Reckoner Shakedown (NEO #119) — {2}{B} Sorcery.
 *   Target opponent reveals their hand. You may choose a nonland card from it. If you do, that
 *   player discards that card. If you don't, put two +1/+1 counters on a creature or Vehicle you
 *   control.
 *
 * Pins the either/or: taking a card discards it and places no counters; declining leaves their hand
 * intact and puts the counters on a creature you choose on resolution.
 */
class ReckonerShakedownScenarioTest : FunSpec({

    fun cast(d: GameTestDriver) {
        val shakedown = d.putCardInHand(d.player1, ReckonerShakedown.name)
        d.giveMana(d.player1, Color.BLACK, 3)
        d.submit(
            CastSpell(
                d.player1, shakedown,
                targets = listOf(ChosenTarget.Player(d.player2)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ReckonerShakedown)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun counters(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("choosing a nonland card makes that player discard it, and no counters are placed") {
        val d = setup()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        cast(d)

        val pick = d.pendingDecision as SelectCardsDecision
        pick.playerId shouldBe d.player1
        d.submitDecision(d.player1, CardsSelectedResponse(pick.id, listOf(bolt)))

        d.pendingDecision shouldBe null
        d.getGraveyardCardNames(d.player2) shouldContain "Lightning Bolt"
        counters(d, bears) shouldBe 0
    }

    test("declining leaves their hand alone and puts two +1/+1 counters on a creature you choose") {
        val d = setup()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val otherBears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        cast(d)

        val pick = d.pendingDecision as SelectCardsDecision
        d.submitDecision(d.player1, CardsSelectedResponse(pick.id, emptyList()))

        when (val pd = d.pendingDecision) {
            is ChooseTargetsDecision -> d.submitDecision(d.player1, TargetsResponse(pd.id, mapOf(0 to listOf(otherBears))))
            is SelectCardsDecision -> d.submitDecision(d.player1, CardsSelectedResponse(pd.id, listOf(otherBears)))
            else -> error("expected a creature choice, got $pd")
        }

        d.getHand(d.player2) shouldContain bolt
        d.getGraveyardCardNames(d.player2) shouldNotContain "Lightning Bolt"
        counters(d, otherBears) shouldBe 2
        counters(d, bears) shouldBe 0
    }
})
