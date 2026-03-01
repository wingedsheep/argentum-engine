package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Decree of Silence.
 *
 * Decree of Silence: {6}{U}{U}
 * Enchantment
 * Whenever an opponent casts a spell, counter that spell and put a depletion counter
 * on Decree of Silence. If there are three or more depletion counters on Decree of
 * Silence, sacrifice it.
 * Cycling {4}{U}{U}
 * When you cycle Decree of Silence, you may counter target spell.
 */
class DecreeOfSilenceTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun setupGame(driver: GameTestDriver): Pair<EntityId, EntityId> {
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return activePlayer to opponent
    }

    fun getDepletionCounters(driver: GameTestDriver, entityId: EntityId): Int {
        return driver.state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.DEPLETION) ?: 0
    }

    test("counters opponent spell and adds depletion counter") {
        val driver = createDriver()
        val (activePlayer, opponent) = setupGame(driver)

        // Put Decree of Silence on the battlefield for active player
        val decree = driver.putPermanentOnBattlefield(activePlayer, "Decree of Silence")

        // Opponent casts a spell
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(activePlayer))

        // Both pass to let the trigger resolve (trigger counters the spell)
        driver.bothPass()

        // Bolt should be countered (in graveyard)
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"

        // Active player should not have taken damage
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Decree should have 1 depletion counter
        getDepletionCounters(driver, decree) shouldBe 1

        // Decree should still be on battlefield
        driver.state.getBattlefield().contains(decree) shouldBe true
    }

    test("sacrifices itself after three depletion counters") {
        val driver = createDriver()
        val (activePlayer, opponent) = setupGame(driver)

        val decree = driver.putPermanentOnBattlefield(activePlayer, "Decree of Silence")

        // Cast 3 opponent spells to accumulate 3 depletion counters
        repeat(3) {
            val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
            driver.giveMana(opponent, Color.RED, 1)
            driver.passPriority(activePlayer)
            driver.castSpell(opponent, bolt, listOf(activePlayer))
            driver.bothPass()
        }

        // All 3 bolts should be countered
        driver.getGraveyardCardNames(opponent).count { it == "Lightning Bolt" } shouldBe 3

        // Decree should have been sacrificed after the 3rd counter
        driver.state.getBattlefield().contains(decree) shouldBe false
        driver.getGraveyardCardNames(activePlayer) shouldContain "Decree of Silence"

        // No damage taken
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("does not counter controller's own spells") {
        val driver = createDriver()
        val (activePlayer, opponent) = setupGame(driver)

        val decree = driver.putPermanentOnBattlefield(activePlayer, "Decree of Silence")

        // Active player casts their own spell — should NOT be countered
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(opponent))
        driver.bothPass()

        // Opponent should take damage (spell was not countered)
        driver.getLifeTotal(opponent) shouldBe 17

        // No depletion counters should have been added
        getDepletionCounters(driver, decree) shouldBe 0
    }
})
