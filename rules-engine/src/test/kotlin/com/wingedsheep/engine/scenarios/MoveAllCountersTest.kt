package com.wingedsheep.engine.scenarios

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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [com.wingedsheep.sdk.scripting.effects.MoveAllCountersEffect] — every counter of every
 * kind leaves the source and lands on the destination (The Ozolith's "move all counters from The
 * Ozolith onto target creature").
 */
class MoveAllCountersTest : FunSpec({

    val mover = card("All Counter Mover") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Move all counters from target creature onto another target creature."
        spell {
            val source = target(TargetFilter.Creature)
            val dest = target(TargetFilter.OtherCreature)
            effect = Effects.MoveAllCounters(source, dest)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(mover))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, id: EntityId): Map<CounterType, Int> =
        driver.state.getEntity(id)?.get<CountersComponent>()?.counters?.filterValues { it > 0 }.orEmpty()

    test("moves every kind of counter, adding to what the destination already has") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val spell = driver.putCardInHand(p1, "All Counter Mover")
        val src = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val dst = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.addComponent(src, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3, CounterType.FLYING to 1)))
        driver.addComponent(dst, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        driver.giveMana(p1, Color.GREEN, 1)

        driver.castSpell(p1, spell, targets = listOf(src, dst))
        driver.bothPass()

        counters(driver, src) shouldBe emptyMap()
        counters(driver, dst) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4, CounterType.FLYING to 1)
    }

    test("a source with no counters moves nothing") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val spell = driver.putCardInHand(p1, "All Counter Mover")
        val src = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val dst = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.giveMana(p1, Color.GREEN, 1)

        driver.castSpell(p1, spell, targets = listOf(src, dst))
        driver.bothPass()

        counters(driver, dst) shouldBe emptyMap()
    }
})
