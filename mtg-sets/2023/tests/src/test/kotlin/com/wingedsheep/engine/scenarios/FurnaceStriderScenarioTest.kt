package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.FurnaceStrider
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Furnace Strider (ONE #133) — {4}{R} 4/5 Creature — Phyrexian Beast.
 *
 * Enters with two oil counters; remove an oil counter: target creature you control gains haste
 * until end of turn.
 */
class FurnaceStriderScenarioTest : FunSpec({

    val hasteId = FurnaceStrider.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FurnaceStrider))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castStrider(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val strider = driver.putCardInHand(player, "Furnace Strider")
        driver.giveMana(player, Color.RED, 5)
        driver.castSpell(player, strider).error shouldBe null
        driver.bothPass()
        return strider
    }

    test("enters with two oil counters and can give itself haste") {
        val driver = newDriver()
        val strider = castStrider(driver)
        oil(driver, strider) shouldBe 2
        driver.state.projectedState.hasKeyword(strider, Keyword.HASTE) shouldBe false

        driver.submitSuccess(
            ActivateAbility(driver.player1, strider, hasteId, targets = listOf(ChosenTarget.Permanent(strider)))
        )
        oil(driver, strider) shouldBe 1
        driver.bothPass()
        driver.state.projectedState.hasKeyword(strider, Keyword.HASTE) shouldBe true
    }

    test("grants haste to another creature you control until end of turn") {
        val driver = newDriver()
        val strider = castStrider(driver)
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        driver.submitSuccess(
            ActivateAbility(driver.player1, strider, hasteId, targets = listOf(ChosenTarget.Permanent(bear)))
        )
        driver.bothPass()
        driver.state.projectedState.hasKeyword(bear, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.hasKeyword(bear, Keyword.HASTE) shouldBe false
    }

    test("cannot target a creature an opponent controls") {
        val driver = newDriver()
        val strider = castStrider(driver)
        val theirBear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.submitExpectFailure(
            ActivateAbility(driver.player1, strider, hasteId, targets = listOf(ChosenTarget.Permanent(theirBear)))
        )
    }

    test("can't activate without an oil counter") {
        val driver = newDriver()
        val strider = driver.putCreatureOnBattlefield(driver.player1, "Furnace Strider")
        oil(driver, strider) shouldBe 0
        driver.submitExpectFailure(
            ActivateAbility(driver.player1, strider, hasteId, targets = listOf(ChosenTarget.Permanent(strider)))
        )
    }
})
