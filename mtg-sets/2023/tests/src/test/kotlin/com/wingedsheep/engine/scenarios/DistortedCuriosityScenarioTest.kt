package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.DistortedCuriosity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Distorted Curiosity (ONE #46) — {2}{U} Sorcery.
 *
 * "Corrupted — This spell costs {2} less to cast if an opponent has three or more poison counters.
 * Draw two cards."
 */
class DistortedCuriosityScenarioTest : FunSpec({

    fun setup(opponentPoison: Int): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DistortedCuriosity))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val players = listOf(driver.player1, driver.player2)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (opponentPoison > 0) {
            driver.addComponent(players[1], CountersComponent(mapOf(CounterType.POISON to opponentPoison)))
        }
        return driver to players
    }

    fun resolveStack(driver: GameTestDriver) {
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)
    }

    test("corrupted: costs only {U} and draws two") {
        val (driver, players) = setup(opponentPoison = 3)
        val you = players[0]
        val spell = driver.putCardInHand(you, "Distorted Curiosity")
        driver.giveMana(you, Color.BLUE, 1)
        val handBefore = driver.getHandSize(you)

        driver.castSpell(you, spell).error shouldBe null
        resolveStack(driver)

        driver.getHandSize(you) shouldBe handBefore - 1 + 2
    }

    test("not corrupted at two poison: {U} alone cannot pay for it") {
        val (driver, players) = setup(opponentPoison = 2)
        val you = players[0]
        val spell = driver.putCardInHand(you, "Distorted Curiosity")
        driver.giveMana(you, Color.BLUE, 1)

        driver.castSpell(you, spell).error.shouldNotBeNull()
    }

    test("not corrupted: full {2}{U} still casts and draws two") {
        val (driver, players) = setup(opponentPoison = 0)
        val you = players[0]
        val spell = driver.putCardInHand(you, "Distorted Curiosity")
        driver.giveMana(you, Color.BLUE, 3)
        val handBefore = driver.getHandSize(you)

        driver.castSpell(you, spell).error shouldBe null
        resolveStack(driver)

        driver.getHandSize(you) shouldBe handBefore - 1 + 2
    }
})
