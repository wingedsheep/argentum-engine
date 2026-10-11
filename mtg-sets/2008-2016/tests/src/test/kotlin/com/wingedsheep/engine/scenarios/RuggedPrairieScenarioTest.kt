package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eve.cards.RuggedPrairie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rugged Prairie: {T}: Add {C}. {R/W}, {T}: Add {R}{R}, {R}{W}, or {W}{W}.
 * Pins that the filter ability is payable with either half of the hybrid symbol and yields the
 * chosen pair, and that the plain tap ability still produces {C}.
 */
class RuggedPrairieScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RuggedPrairie)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun activate(driver: GameTestDriver, abilityIndex: Int, payWith: Color) {
        val player = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(player, "Rugged Prairie")
        driver.giveMana(player, payWith, 1) // pays the {R/W} cost
        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = land,
                abilityId = RuggedPrairie.activatedAbilities[abilityIndex].id,
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
        val land = driver.putPermanentOnBattlefield(player, "Rugged Prairie")
        driver.submit(
            ActivateAbility(playerId = player, sourceId = land, abilityId = RuggedPrairie.activatedAbilities[0].id)
        ).outcome shouldBe Outcome.Done
        pool(driver).colorless shouldBe 1
    }

    test("paying {R/W} with {R} for {R}{W} leaves one of each in the pool") {
        val driver = setup()
        activate(driver, abilityIndex = 2, payWith = Color.RED)
        pool(driver).red shouldBe 1
        pool(driver).white shouldBe 1
    }

    test("paying {R/W} with {W} for {R}{R} converts colours") {
        val driver = setup()
        activate(driver, abilityIndex = 1, payWith = Color.WHITE)
        pool(driver).red shouldBe 2
        pool(driver).white shouldBe 0
    }
})
