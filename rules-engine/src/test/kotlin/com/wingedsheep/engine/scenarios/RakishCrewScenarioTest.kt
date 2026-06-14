package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RakishCrew
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rakish Crew — {2}{B} Enchantment.
 *
 * "When this enchantment enters, create a 1/1 red Mercenary creature token ..."
 * "Whenever an outlaw you control dies, each opponent loses 1 life and you gain 1 life."
 *
 * Verifies the ETB makes a Mercenary token (an outlaw you control) and that when that outlaw
 * dies, the drain fires: each opponent loses 1 life and you gain 1 life.
 */
class RakishCrewScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RakishCrew)
        return driver
    }

    test("Mercenary token death drains each opponent for 1 and gains you 1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tokensBefore = driver.state.getBattlefield().filter {
            val e = driver.state.getEntity(it)
            e?.has<TokenComponent>() == true && e.get<ControllerComponent>()?.playerId == me
        }.toSet()

        // Cast Rakish Crew, resolve it and its ETB token trigger.
        val crew = driver.putCardInHand(me, "Rakish Crew")
        driver.giveMana(me, Color.BLACK, 3)
        driver.castSpell(me, crew).isSuccess shouldBe true
        var guard = 0
        while (driver.stackSize > 0 && !driver.isPaused && guard++ < 10) driver.bothPass()

        val token = (driver.state.getBattlefield().filter {
            val e = driver.state.getEntity(it)
            e?.has<TokenComponent>() == true && e.get<ControllerComponent>()?.playerId == me
        }.toSet() - tokensBefore).single()

        val myLifeBefore = driver.getLifeTotal(me)
        val oppLifeBefore = driver.getLifeTotal(opp)

        // Kill the Mercenary token (an outlaw I control) with Lightning Bolt.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(token))
        // Resolve Bolt; SBAs then kill the token, putting the drain trigger on the stack;
        // keep passing until the stack settles (bolt -> death -> drain trigger).
        guard = 0
        while ((driver.stackSize > 0 || driver.isPaused) && guard++ < 20) driver.bothPass()

        // Outlaw died → opponent loses 1, I gain 1.
        driver.getLifeTotal(opp) shouldBe oppLifeBefore - 1
        driver.getLifeTotal(me) shouldBe myLifeBefore + 1
    }
})
