package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c13.cards.Ophiomancer
import com.wingedsheep.mtg.sets.definitions.mmq.cards.BoaConstrictor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Ophiomancer (C13) — {2}{B} Creature — Human Shaman 2/2.
 *
 * "At the beginning of each upkeep, if you control no Snakes, create a 1/1 black Snake creature
 * token with deathtouch."
 *
 * Pins: every player's upkeep (not just yours), the intervening-if at trigger time and again at
 * resolution (CR 603.4), and that any Snake you control counts while an opponent's doesn't.
 */
class OphiomancerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Ophiomancer, BoaConstrictor))
        return driver
    }

    fun advanceToUpkeepOf(driver: GameTestDriver, player: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (driver.activePlayer != player) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe player
    }

    fun snakeTokens(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getPermanents(player).filter { id ->
            driver.state.getEntity(id)?.has<TokenComponent>() == true &&
                driver.state.projectedState.hasSubtype(id, "Snake")
        }

    test("creates a 1/1 black deathtouch Snake on its controller's upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        // Land Ophiomancer during the opponent's turn so the next upkeep is the controller's own
        // (otherwise the opponent's upkeep makes the Snake first and the controller's never triggers).
        advanceToUpkeepOf(driver, opponent)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(controller, "Ophiomancer")

        advanceToUpkeepOf(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass()

        val snakes = snakeTokens(driver, controller)
        snakes shouldHaveSize 1
        val snake = snakes.single()
        val projected = driver.state.projectedState
        projected.getPower(snake) shouldBe 1
        projected.getToughness(snake) shouldBe 1
        projected.getColors(snake) shouldBe setOf(Color.BLACK.name)
        projected.hasKeyword(snake, Keyword.DEATHTOUCH) shouldBe true
    }

    test("also triggers on an opponent's upkeep, and an opponent's Snake doesn't stop it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(controller, "Ophiomancer")
        driver.putCreatureOnBattlefield(opponent, "Boa Constrictor")

        advanceToUpkeepOf(driver, opponent)
        driver.stackSize shouldBe 1
        driver.bothPass()

        snakeTokens(driver, controller) shouldHaveSize 1
        snakeTokens(driver, opponent) shouldHaveSize 0
    }

    test("does not trigger while you control a Snake") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(controller, "Ophiomancer")
        // A non-token Snake counts too — "Snakes" is any Snake you control.
        driver.putCreatureOnBattlefield(controller, "Boa Constrictor")

        advanceToUpkeepOf(driver, opponent)
        driver.stackSize shouldBe 0
        snakeTokens(driver, controller) shouldHaveSize 0
    }

    test("does nothing if you gain a Snake before it resolves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(controller, "Ophiomancer")

        advanceToUpkeepOf(driver, opponent)
        driver.stackSize shouldBe 1
        driver.putCreatureOnBattlefield(controller, "Boa Constrictor")
        driver.bothPass()

        driver.stackSize shouldBe 0
        snakeTokens(driver, controller) shouldHaveSize 0
    }
})
