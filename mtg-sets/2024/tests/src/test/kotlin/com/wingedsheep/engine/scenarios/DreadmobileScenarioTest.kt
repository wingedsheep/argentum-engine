package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Dreadmobile
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dreadmobile (MH3 #87) — {2}{B} Artifact — Vehicle 3/3.
 * Menace. {1}, Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle. Crew 1.
 */
class DreadmobileScenarioTest : FunSpec({

    val abilityId = Dreadmobile.activatedAbilities.first().id
    val widget = CardDefinition.artifact(name = "Test Widget", manaCost = ManaCost.parse("{1}"))

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Dreadmobile, widget))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("sacrificing another creature puts a +1/+1 counter on the Vehicle, which shows once crewed") {
        val driver = newDriver()
        val me = driver.player1

        val mobile = driver.putPermanentOnBattlefield(me, "Dreadmobile")
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val crewer = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(crewer)

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = mobile,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        ).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.getBattlefield().contains(fodder) shouldBe false
        driver.state.getEntity(mobile)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

        // Crew 1 with a 2-power creature → it becomes a 4/4 artifact creature.
        driver.submit(CrewVehicle(me, mobile, listOf(crewer))).outcome shouldBe Outcome.Done
        driver.bothPass()
        val projected = driver.state.projectedState
        projected.isCreature(mobile) shouldBe true
        projected.getPower(mobile) shouldBe 4
        projected.getToughness(mobile) shouldBe 4
    }

    test("a noncreature artifact can be sacrificed too") {
        val driver = newDriver()
        val me = driver.player1

        val mobile = driver.putPermanentOnBattlefield(me, "Dreadmobile")
        val fodder = driver.putPermanentOnBattlefield(me, "Test Widget")

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = mobile,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        ).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.getBattlefield().contains(fodder) shouldBe false
        driver.state.getEntity(mobile)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("the Vehicle cannot sacrifice itself to its own ability") {
        val driver = newDriver()
        val me = driver.player1

        val mobile = driver.putPermanentOnBattlefield(me, "Dreadmobile")
        driver.giveColorlessMana(me, 1)
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = mobile,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(mobile))
            )
        )
        driver.state.getBattlefield().contains(mobile) shouldBe true
    }
})
