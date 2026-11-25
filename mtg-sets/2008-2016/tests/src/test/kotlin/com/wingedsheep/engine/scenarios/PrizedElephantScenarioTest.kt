package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.PrizedElephant
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Prized Elephant {3}{W} — Creature — Elephant 3/3
 *   This creature gets +1/+1 as long as you control a Forest.
 *   {G}: This creature gains trample until end of turn.
 */
class PrizedElephantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PrizedElephant))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("is 4/4 only while you control a Forest — an opponent's Forest doesn't count") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val elephant = driver.putCreatureOnBattlefield(me, "Prized Elephant")
        driver.putLandOnBattlefield(opp, "Forest")
        driver.state.projectedState.getPower(elephant) shouldBe 3
        driver.state.projectedState.getToughness(elephant) shouldBe 3

        driver.putLandOnBattlefield(me, "Forest")
        driver.state.projectedState.getPower(elephant) shouldBe 4
        driver.state.projectedState.getToughness(elephant) shouldBe 4
    }

    test("{G} gives it trample until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val elephant = driver.putCreatureOnBattlefield(me, "Prized Elephant")
        driver.putLandOnBattlefield(me, "Forest")
        driver.state.projectedState.hasKeyword(elephant, Keyword.TRAMPLE) shouldBe false

        val abilityId = PrizedElephant.script.activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = elephant, abilityId = abilityId))
        driver.bothPass()
        driver.state.projectedState.hasKeyword(elephant, Keyword.TRAMPLE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP, driver.getOpponent(me))
        driver.state.projectedState.hasKeyword(elephant, Keyword.TRAMPLE) shouldBe false
    }
})
