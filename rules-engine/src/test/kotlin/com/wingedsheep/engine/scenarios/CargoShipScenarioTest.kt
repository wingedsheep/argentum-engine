package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CargoShip
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/** Scenario tests for Cargo Ship. */
class CargoShipScenarioTest : FunSpec({

    fun createDriver(vararg cards: com.wingedsheep.sdk.model.CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        cards.forEach { driver.registerCard(it) }
        return driver
    }

    // -----------------------------------------------------------------------------------------
    // Cargo Ship
    // -----------------------------------------------------------------------------------------

    test("Cargo Ship has flying and vigilance and taps for restricted mana") {
        val driver = createDriver(CargoShip)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val ship = driver.putPermanentOnBattlefield(active, "Cargo Ship")

        driver.state.projectedState.hasKeyword(ship, Keyword.FLYING).shouldBeTrue()
        driver.state.projectedState.hasKeyword(ship, Keyword.VIGILANCE).shouldBeTrue()

        // Activating the {T}: Add {C} mana ability taps the Vehicle.
        val manaAbility = CargoShip.activatedAbilities.first { it.isManaAbility }
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = ship,
                abilityId = manaAbility.id,
            )
        )
        result.isSuccess shouldBe true
        driver.isTapped(ship) shouldBe true
    }
})
