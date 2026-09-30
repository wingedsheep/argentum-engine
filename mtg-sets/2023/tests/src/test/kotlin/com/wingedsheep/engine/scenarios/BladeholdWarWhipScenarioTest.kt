package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.BladeholdWarWhip
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bladehold War-Whip (ONE) — "Equip abilities you activate of other Equipment cost {1} less to
 * activate" ([ReduceEquipCost] with `onlyOtherEquip = true`), plus double strike for the equipped
 * creature. The discount is pinned with an inline Equip {3} blade, and its own Equip {3}{R}{W} is
 * shown to stay full price.
 */
class BladeholdWarWhipScenarioTest : FunSpec({

    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equip {3}"
        equipAbility("{3}")
    }
    val bladeEquipId = testBlade.activatedAbilities.first().id
    val whipEquipId = BladeholdWarWhip.activatedAbilities.first { it.isEquipAbility }.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BladeholdWarWhip, testBlade))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    test("another Equipment's Equip {3} costs {2}") {
        val driver = createDriver()
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putPermanentOnBattlefield(driver.player1, "Bladehold War-Whip")
        val blade = driver.putPermanentOnBattlefield(driver.player1, "Test Blade")
        driver.advanceToPlayer1Main()

        driver.giveColorlessMana(driver.player1, 2)
        driver.submit(
            ActivateAbility(driver.player1, blade, bladeEquipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear
    }

    test("its own equip ability is not discounted, and grants double strike") {
        val driver = createDriver()
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val whip = driver.putPermanentOnBattlefield(driver.player1, "Bladehold War-Whip")
        driver.advanceToPlayer1Main()

        // {2}{R}{W} would only cover the cost if the War-Whip discounted itself.
        driver.giveColorlessMana(driver.player1, 2)
        driver.giveMana(driver.player1, Color.RED)
        driver.giveMana(driver.player1, Color.WHITE)
        driver.submit(
            ActivateAbility(driver.player1, whip, whipEquipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldNotBe Outcome.Done

        driver.giveColorlessMana(driver.player1, 1)
        driver.submit(
            ActivateAbility(driver.player1, whip, whipEquipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(whip)?.get<AttachedToComponent>()?.targetId shouldBe bear
        StateProjector().project(driver.state).hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
