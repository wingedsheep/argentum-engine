package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltc.cards.ArwenWeaverOfHope
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Arwen, Weaver of Hope — "Each other creature you control enters with a number of additional
 * +1/+1 counters on it equal to Arwen's toughness."
 *
 * The count names the replacement's source (Arwen), not the entering creature, and reads her
 * toughness as the creature enters — pumps included (ruling 2023-06-23).
 */
class ArwenWeaverOfHopeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArwenWeaverOfHope))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOnes(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun castBears(driver: GameTestDriver, player: EntityId): EntityId {
        val bear = driver.putCardInHand(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, bear)
        driver.bothPass()
        return driver.getCreatures(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
    }

    test("another creature you control enters with counters equal to Arwen's printed toughness") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Arwen, Weaver of Hope")

        plusOnes(driver, castBears(driver, me)) shouldBe 1
    }

    test("the count reads Arwen's current toughness, pumps included") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val arwen = driver.putCreatureOnBattlefield(me, "Arwen, Weaver of Hope")

        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, growth, listOf(arwen))
        driver.bothPass()

        plusOnes(driver, castBears(driver, me)) shouldBe 4
    }

    test("Arwen entering does not put counters on herself") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val arwen = driver.putCardInHand(me, "Arwen, Weaver of Hope")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, arwen)
        driver.bothPass()

        plusOnes(driver, arwen) shouldBe 0
    }

    test("an opponent's Arwen gives your creatures nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Arwen, Weaver of Hope")

        plusOnes(driver, castBears(driver, me)) shouldBe 0
    }
})
