package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Corrupted — "if an opponent has three or more poison counters" — and its per-player sibling
 * [Conditions.PoisonCountersAtLeast]. The cases that matter are multiplayer: the test is
 * existential over opponents (one poisoned opponent is enough, no matter the seat order), it is
 * not a sum across opponents, and your own poison never counts.
 */
class CorruptedConditionTest : FunSpec({

    fun threePlayers(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        val players = driver.initMultiplayer(List(3) { Deck.of("Forest" to 20) }, skipMulligans = true)
        return driver to players
    }

    fun GameTestDriver.poison(player: EntityId, n: Int) =
        addComponent(player, CountersComponent(mapOf(CounterType.POISON to n)))

    fun GameTestDriver.ctx(controller: EntityId) =
        EffectContext(sourceId = null, controllerId = controller, targets = emptyList(), xValue = 0)

    fun GameTestDriver.holds(condition: com.wingedsheep.sdk.scripting.conditions.Condition, controller: EntityId) =
        PredicateEvaluator(cardRegistry = null).conditions.evaluate(state, condition, ctx(controller))

    test("one opponent at three poison is enough, whichever seat it is") {
        val (driver, p) = threePlayers()
        driver.holds(Conditions.Corrupted, p[0]) shouldBe false
        driver.poison(p[2], 3)
        driver.holds(Conditions.Corrupted, p[0]) shouldBe true
    }

    test("poison split across opponents doesn't add up to corrupted") {
        val (driver, p) = threePlayers()
        driver.poison(p[1], 2)
        driver.poison(p[2], 2)
        driver.holds(Conditions.Corrupted, p[0]) shouldBe false
    }

    test("your own poison counters never make you corrupted") {
        val (driver, p) = threePlayers()
        driver.poison(p[0], 9)
        driver.holds(Conditions.Corrupted, p[0]) shouldBe false
        driver.holds(Conditions.PoisonCountersAtLeast(3), p[0]) shouldBe true
    }

    test("PlayerCounterCount over EachOpponent sums every opponent, not just the first") {
        val (driver, p) = threePlayers()
        driver.poison(p[1], 1)
        driver.poison(p[2], 4)
        PredicateEvaluator(cardRegistry = null).amounts.evaluate(
            driver.state,
            DynamicAmount.PlayerCounterCount(CounterType.POISON, Player.EachOpponent),
            driver.ctx(p[0]),
        ) shouldBe 5
    }
})
