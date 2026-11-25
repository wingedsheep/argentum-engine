package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.Chronomaton
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chronomaton {1} — Artifact Creature — Golem 1/1
 *   {1}, {T}: Put a +1/+1 counter on this creature.
 */
class ChronomatonScenarioTest : FunSpec({

    test("{1}, {T} taps it and puts a +1/+1 counter on it") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Chronomaton))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.putLandOnBattlefield(me, "Swamp")
        val golem = driver.putCreatureOnBattlefield(me, "Chronomaton")
        driver.removeSummoningSickness(golem)

        val abilityId = Chronomaton.script.activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = golem, abilityId = abilityId))
        driver.bothPass()

        driver.isTapped(golem) shouldBe true
        driver.state.getEntity(golem)!!.get<CountersComponent>()!!
            .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.state.projectedState.getPower(golem) shouldBe 2
        driver.state.projectedState.getToughness(golem) shouldBe 2
    }
})
