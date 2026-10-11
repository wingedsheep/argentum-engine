package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eve.cards.TwilightMire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Twilight Mire: {T}: Add {C}. {B/G}, {T}: Add {B}{B}, {B}{G}, or {G}{G}.
 * Pins that the filter ability is payable with either half of the hybrid symbol and yields the
 * chosen pair, and that the plain tap ability still produces {C}.
 */
class TwilightMireScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TwilightMire)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun activate(driver: GameTestDriver, abilityIndex: Int, payWith: Color) {
        val player = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(player, "Twilight Mire")
        driver.giveMana(player, payWith, 1) // pays the {B/G} cost
        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = land,
                abilityId = TwilightMire.activatedAbilities[abilityIndex].id,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        result.outcome shouldBe Outcome.Done
        driver.isTapped(land) shouldBe true
    }

    fun pool(driver: GameTestDriver) = driver.state.getEntity(driver.activePlayer!!)!!.get<ManaPoolComponent>()!!

    test("{T}: Add {C}") {
        val driver = setup()
        val player = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(player, "Twilight Mire")
        driver.submit(
            ActivateAbility(playerId = player, sourceId = land, abilityId = TwilightMire.activatedAbilities[0].id)
        ).outcome shouldBe Outcome.Done
        pool(driver).colorless shouldBe 1
    }

    test("paying {B/G} with {B} for {B}{G} leaves one of each in the pool") {
        val driver = setup()
        activate(driver, abilityIndex = 2, payWith = Color.BLACK)
        pool(driver).black shouldBe 1
        pool(driver).green shouldBe 1
    }

    test("paying {B/G} with {G} for {B}{B} converts colours") {
        val driver = setup()
        activate(driver, abilityIndex = 1, payWith = Color.GREEN)
        pool(driver).black shouldBe 2
        pool(driver).green shouldBe 0
    }
})
