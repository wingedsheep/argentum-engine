package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `PlayerCounterCount` with a `null` counter type counts every kind of counter a player has —
 * including kinds no card in the corpus names, because `CounterType` is open (CR 122.1 lets any
 * kind of counter sit on a player). Added with it: the "each counter among players and
 * permanents" composition (Lumbering Megasloth), which adds the battlefield's all-kinds sum.
 */
class PlayerCounterCountAllKindsTest : FunSpec({

    val oddKind = CounterType("TEST_ODDITY")

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 20), skipMulligans = true)
    }

    fun GameTestDriver.counters(id: EntityId, vararg kinds: Pair<CounterType, Int>) =
        addComponent(id, CountersComponent(mapOf(*kinds)))

    fun GameTestDriver.eval(amount: DynamicAmount, controller: EntityId = player1): Int =
        PredicateEvaluator(cardRegistry = null).conditions.amounts.evaluate(
            state, amount,
            EffectContext(sourceId = null, controllerId = controller, targets = emptyList(), xValue = 0),
        )

    test("null counter type sums every kind a player has, including an unnamed kind") {
        val d = driver()
        d.counters(d.player1, CounterType.ENERGY to 3, CounterType.POISON to 2, oddKind to 1)
        d.eval(DynamicAmounts.playerCounterCount(null, Player.You)) shouldBe 6
        d.eval(DynamicAmounts.playerCounterCount(CounterType.ENERGY, Player.You)) shouldBe 3
    }

    test("a multi-player scope sums every seat; You and EachOpponent stay separate") {
        val d = driver()
        d.counters(d.player1, CounterType.ENERGY to 2)
        d.counters(d.player2, CounterType.POISON to 4, oddKind to 1)
        d.eval(DynamicAmounts.playerCounterCount(null, Player.You)) shouldBe 2
        d.eval(DynamicAmounts.playerCounterCount(null, Player.EachOpponent)) shouldBe 5
        d.eval(DynamicAmounts.playerCounterCount(null, Player.Each)) shouldBe 7
    }

    test("no counters anywhere counts zero") {
        val d = driver()
        d.eval(DynamicAmounts.playerCounterCount(null, Player.Each)) shouldBe 0
    }

    test("counters among players and permanents adds both sides, either controller") {
        val d = driver()
        val mine = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.counters(mine, CounterType.PLUS_ONE_PLUS_ONE to 2, oddKind to 1)
        d.counters(theirs, CounterType.MINUS_ONE_MINUS_ONE to 1)
        d.counters(d.player2, CounterType.ENERGY to 4)
        val amongPlayersAndPermanents =
            DynamicAmounts.playerCounterCount(null, Player.Each) + DynamicAmounts.battlefield(Player.Each).totalCounters()
        d.eval(amongPlayersAndPermanents) shouldBe 8
        d.eval(amongPlayersAndPermanents, controller = d.player2) shouldBe 8
    }

    test("description reads as the whole count") {
        DynamicAmount.PlayerCounterCount(null, Player.You).description shouldBe "the number of counters you have"
        DynamicAmount.PlayerCounterCount(CounterType.POISON, Player.You).description shouldBe
            "${Player.You.possessive} ${CounterType.POISON.printed} counters"
    }
})
