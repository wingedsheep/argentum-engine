package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.FlywheelRacer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flywheel Racer (MOM #259) — "{T}: Add one mana of any color. Activate only if this permanent
 * is a creature." plus Crew 1.
 *
 * Pins the activation gate: an uncrewed Racer (a noncreature artifact) can't tap for mana, a
 * crewed one can.
 */
class FlywheelRacerScenarioTest : FunSpec({

    val manaAbilityId = FlywheelRacer.activatedAbilities.first { it.isManaAbility }.id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FlywheelRacer)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("an uncrewed Racer is not a creature and can't tap for mana") {
        val driver = newDriver()
        val racer = driver.putPermanentOnBattlefield(driver.player1, "Flywheel Racer")
        driver.removeSummoningSickness(racer)

        val result = driver.submit(ActivateAbility(driver.player1, racer, manaAbilityId, manaColorChoice = Color.RED))
        result.outcome shouldNotBe Outcome.Done
        driver.pool(driver.player1).red shouldBe 0
        driver.state.getEntity(racer)?.has<TappedComponent>() shouldBe false
    }

    test("once crewed, the Racer taps for one mana of any color") {
        val driver = newDriver()
        val racer = driver.putPermanentOnBattlefield(driver.player1, "Flywheel Racer")
        val crewer = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(racer)

        driver.submitSuccess(CrewVehicle(driver.player1, racer, listOf(crewer)))
        driver.bothPass()
        driver.state.projectedState.isCreature(racer) shouldBe true

        driver.submit(
            ActivateAbility(driver.player1, racer, manaAbilityId, manaColorChoice = Color.RED)
        ).outcome shouldBe Outcome.Done
        driver.pool(driver.player1).red shouldBe 1
        driver.state.getEntity(racer)?.has<TappedComponent>() shouldBe true
    }
})
