package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.LeoninWarleader
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Leonin Warleader (M19 #23) — "Whenever this creature attacks, create two 1/1 white Cat creature
 * tokens with lifelink that are tapped and attacking."
 */
class LeoninWarleaderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeoninWarleader))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("attacking creates two tapped, attacking 1/1 lifelink Cats that deal damage and gain life") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val warleader = driver.putCreatureOnBattlefield(attacker, "Leonin Warleader")
        driver.removeSummoningSickness(warleader)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(warleader), defender)

        var safety = 0
        while (driver.state.getBattlefield().count { id ->
                driver.state.projectedState.hasSubtype(id, "Cat") && id != warleader
            } < 2 && safety++ < 10
        ) {
            driver.bothPass()
        }

        val cats = driver.state.getBattlefield().filter { id ->
            id != warleader && driver.state.projectedState.hasSubtype(id, "Cat")
        }
        cats shouldHaveSize 2
        val projected = driver.state.projectedState
        for (cat in cats) {
            projected.getPower(cat) shouldBe 1
            projected.getToughness(cat) shouldBe 1
            projected.hasKeyword(cat, Keyword.LIFELINK) shouldBe true
            driver.state.getEntity(cat)?.has<TappedComponent>() shouldBe true
            driver.state.getEntity(cat)?.get<AttackingComponent>()?.defenderId shouldBe defender
        }

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.assertLifeTotal(defender, 14)
        driver.assertLifeTotal(attacker, 22)
    }
})
