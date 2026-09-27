package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HeartbeatOfSpring
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Heartbeat of Spring ({2}{G} Enchantment):
 * "Whenever a player taps a land for mana, that player adds one mana of any type that land produced."
 */
class HeartbeatOfSpringScenarioTest : FunSpec({

    val TestForest = basicLand("Forest") {}
    val TestMountain = basicLand("Mountain") {}
    val cards = TestCards.all + listOf(TestForest, TestMountain, HeartbeatOfSpring)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("tapping your own land adds one extra mana of the type it produced") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Heartbeat of Spring")
        val mountain = driver.putPermanentOnBattlefield(me, "Mountain")

        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = mountain, abilityId = TestMountain.activatedAbilities[0].id)
        )
        result.outcome shouldBe Outcome.Done

        val pool = driver.state.getEntity(me)?.get<ManaPoolComponent>()!!
        pool.red shouldBe 2
        pool.green shouldBe 0
    }

    test("affects every player, not just its controller") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(opponent, "Heartbeat of Spring")
        val forest = driver.putPermanentOnBattlefield(me, "Forest")

        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = forest, abilityId = TestForest.activatedAbilities[0].id)
        )
        result.outcome shouldBe Outcome.Done

        driver.state.getEntity(me)?.get<ManaPoolComponent>()!!.green shouldBe 2
        driver.state.getEntity(opponent)?.get<ManaPoolComponent>()!!.green shouldBe 0
    }

    test("no bonus without Heartbeat of Spring on the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val forest = driver.putPermanentOnBattlefield(me, "Forest")

        driver.submit(
            ActivateAbility(playerId = me, sourceId = forest, abilityId = TestForest.activatedAbilities[0].id)
        ).outcome shouldBe Outcome.Done

        driver.state.getEntity(me)?.get<ManaPoolComponent>()!!.green shouldBe 1
    }

    test("the mana solver counts the doubled mana for automatic payment") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Heartbeat of Spring")
        driver.putPermanentOnBattlefield(me, "Mountain")

        val registry = CardRegistry().apply { register(cards) }
        val solver = ManaSolver(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        solver.canPay(driver.state, me, ManaCost.parse("{R}{R}")) shouldBe true
        solver.canPay(driver.state, me, ManaCost.parse("{R}{R}{R}")) shouldBe false
    }
})
