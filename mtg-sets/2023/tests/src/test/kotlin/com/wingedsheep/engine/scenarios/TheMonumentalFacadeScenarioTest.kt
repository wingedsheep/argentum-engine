package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.TheMonumentalFacade
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Monumental Facade (ONE #255) — Land — Sphere.
 *
 * "This land enters with two oil counters on it.
 *  {T}: Add {C}.
 *  {T}, Remove an oil counter from this land: Put an oil counter on target artifact or creature
 *  you control. Activate only as a sorcery."
 */
class TheMonumentalFacadeScenarioTest : FunSpec({

    val oilAbility = TheMonumentalFacade.activatedAbilities[1].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheMonumentalFacade))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun playFacade(driver: GameTestDriver): EntityId {
        val card = driver.putCardInHand(driver.player1, "The Monumental Facade")
        driver.playLand(driver.player1, card).outcome shouldBe Outcome.Done
        return card
    }

    test("enters with two oil counters") {
        val driver = newDriver()
        val facade = playFacade(driver)
        oil(driver, facade) shouldBe 2
    }

    test("tap and remove an oil counter: move an oil counter onto a creature you control") {
        val driver = newDriver()
        val facade = playFacade(driver)
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")

        driver.submitSuccess(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = facade,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(courser)),
            )
        )
        driver.isTapped(facade) shouldBe true
        oil(driver, facade) shouldBe 1
        driver.bothPass()
        oil(driver, courser) shouldBe 1
    }

    test("can't target a creature an opponent controls") {
        val driver = newDriver()
        val facade = playFacade(driver)
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = facade,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(theirs)),
            )
        )
        oil(driver, facade) shouldBe 2
    }

    test("can't be activated without an oil counter to remove") {
        val driver = newDriver()
        // Put onto the battlefield directly — bypasses the enters-with-counters replacement.
        val facade = driver.putPermanentOnBattlefield(driver.player1, "The Monumental Facade")
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        oil(driver, facade) shouldBe 0

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = facade,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(courser)),
            )
        )
        oil(driver, courser) shouldBe 0
    }

    test("oil ability is sorcery-speed only") {
        val driver = newDriver()
        val facade = playFacade(driver)
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = facade,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(courser)),
            )
        )
        oil(driver, facade) shouldBe 2
        oil(driver, courser) shouldBe 0
    }
})
