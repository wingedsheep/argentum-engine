package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Proliferate is "any number of" recipients, each its own choice. The generic select-cards
 * fallback answered it all-or-nothing, so the AI proliferated a human opponent's +1/+1 and
 * loyalty counters along with its own.
 */
class ProliferateRecipientsAiTest : FunSpec({
    val proliferate = card("AI Test Proliferate") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.Proliferate() }
    }
    val body = card("AI Test Proliferate Body") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
    }

    fun GameTestDriver.counters(id: EntityId, type: CounterType, n: Int) =
        replaceState(state.updateEntity(id) { it.with(CountersComponent(mapOf(type to n))) })

    fun GameTestDriver.count(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("AI proliferates its own +1/+1 counters and the opponent's -1/-1 and poison, never the opponent's +1/+1") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(proliferate, body))
            initMirrorMatch(Deck.of("Island" to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val ai = d.activePlayer!!
        val human = d.getOpponent(ai)

        val mine = d.putPermanentOnBattlefield(ai, body.name).also { d.counters(it, CounterType.PLUS_ONE_PLUS_ONE, 1) }
        val theirsGrown = d.putPermanentOnBattlefield(human, body.name).also { d.counters(it, CounterType.PLUS_ONE_PLUS_ONE, 2) }
        val theirsShrunk = d.putPermanentOnBattlefield(human, body.name).also { d.counters(it, CounterType.MINUS_ONE_MINUS_ONE, 1) }
        d.counters(human, CounterType.POISON, 3)

        val spell = d.putCardInHand(ai, proliferate.name)
        d.giveMana(ai, Color.BLUE, 1)
        d.castSpell(ai, spell).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision as SelectCardsDecision
        decision.options shouldContainExactlyInAnyOrder listOf(mine, theirsGrown, theirsShrunk, human)

        val responder = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator())
        val answer = responder.respond(d.state, decision, ai) as CardsSelectedResponse
        answer.selectedCards shouldContainExactlyInAnyOrder listOf(mine, theirsShrunk, human)

        d.submitCardSelection(ai, answer.selectedCards).error shouldBe null
        d.count(mine, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.count(theirsGrown, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.count(theirsShrunk, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
        d.count(human, CounterType.POISON) shouldBe 4
    }
})
