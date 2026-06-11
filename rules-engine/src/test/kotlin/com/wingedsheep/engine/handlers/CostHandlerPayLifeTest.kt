package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.AdditionalCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Direct tests for CR 119.4 — paying life requires a life total greater than or
 * equal to the amount paid. Pre-fix this engine used `life > amount`, rejecting the
 * legal "pay X life at exactly X life" case (e.g. Bitter Triumph's "pay 3 life" with
 * the caster at 3 life).
 *
 * Two slots exercise the same rule:
 *  - [AbilityCost.PayLife] — used by activated abilities (Channel-style and friends)
 *  - [AdditionalCost.PayLife] — used by spell additional costs (Bitter Triumph, etc.)
 */
class CostHandlerPayLifeTest : FunSpec({

    fun createDriver(initialLife: Int): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        driver.setLifeTotal(player, initialLife)
        return driver to player
    }

    context("AbilityCost.PayLife — CR 119.4 boundary") {
        test("life == cost is payable (the boundary case)") {
            val (driver, player) = createDriver(initialLife = 3)
            CostHandler().canPayAbilityCost(
                state = driver.state,
                cost = AbilityCost.PayLife(3),
                sourceId = player,
                controllerId = player,
                manaPool = ManaPool()
            ) shouldBe true
        }

        test("life > cost is payable") {
            val (driver, player) = createDriver(initialLife = 20)
            CostHandler().canPayAbilityCost(
                state = driver.state,
                cost = AbilityCost.PayLife(3),
                sourceId = player,
                controllerId = player,
                manaPool = ManaPool()
            ) shouldBe true
        }

        test("life < cost is NOT payable") {
            val (driver, player) = createDriver(initialLife = 2)
            CostHandler().canPayAbilityCost(
                state = driver.state,
                cost = AbilityCost.PayLife(3),
                sourceId = player,
                controllerId = player,
                manaPool = ManaPool()
            ) shouldBe false
        }
    }

    context("AdditionalCost.PayLife — CR 119.4 boundary (Bitter Triumph 'pay 3 life' mode)") {
        test("life == cost is payable (the boundary case)") {
            val (driver, player) = createDriver(initialLife = 3)
            CostHandler().canPayAdditionalCost(
                state = driver.state,
                cost = Costs.additional.PayLife(3),
                controllerId = player
            ) shouldBe true
        }

        test("life > cost is payable") {
            val (driver, player) = createDriver(initialLife = 20)
            CostHandler().canPayAdditionalCost(
                state = driver.state,
                cost = Costs.additional.PayLife(3),
                controllerId = player
            ) shouldBe true
        }

        test("life < cost is NOT payable") {
            val (driver, player) = createDriver(initialLife = 2)
            CostHandler().canPayAdditionalCost(
                state = driver.state,
                cost = Costs.additional.PayLife(3),
                controllerId = player
            ) shouldBe false
        }
    }
})
