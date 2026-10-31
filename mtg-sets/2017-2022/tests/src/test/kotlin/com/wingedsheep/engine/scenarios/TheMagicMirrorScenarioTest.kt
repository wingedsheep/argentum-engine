package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.TheMagicMirror
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Magic Mirror — "You have no maximum hand size. At the beginning of your upkeep, put a knowledge
 * counter on The Magic Mirror, then draw a card for each knowledge counter on The Magic Mirror."
 */
class TheMagicMirrorScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(TheMagicMirror)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.knowledge(mirror: EntityId): Int =
        state.getEntity(mirror)?.get<CountersComponent>()?.getCount(CounterType.KNOWLEDGE) ?: 0

    test("each of your upkeeps adds a knowledge counter, then draws one card per counter") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        val mirror = d.putPermanentOnBattlefield(you, "The Magic Mirror")
        d.replaceState(d.state.updateEntity(mirror) { it.with(CountersComponent().withAdded(CounterType.KNOWLEDGE, 2)) })

        d.passPriorityUntil(Step.DRAW, opp)
        d.knowledge(mirror) shouldBe 2 // not on the opponent's upkeep

        d.passPriorityUntil(Step.UPKEEP, you)
        val hand = d.getHandSize(you)
        d.bothPass() // resolve the upkeep trigger
        d.knowledge(mirror) shouldBe 3
        d.getHandSize(you) shouldBe hand + 3
    }

    test("you have no maximum hand size; the opponent keeps theirs") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "The Magic Mirror")
        val max = { p: EntityId ->
            MaximumHandSize.effective(d.state, p, d.cardRegistry, d.services.conditionEvaluator, d.services.dynamicAmountEvaluator)
        }
        max(you) shouldBe null
        max(opp) shouldBe 7
    }
})
