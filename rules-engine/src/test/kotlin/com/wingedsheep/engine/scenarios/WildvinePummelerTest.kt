package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.WildvinePummeler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Wildvine Pummeler's Vivid cost reduction.
 *
 * Wildvine Pummeler: {6}{G}
 * Creature — Giant Berserker
 * 6/5
 * Vivid — This spell costs {1} less to cast for each color among permanents you control.
 * Reach, trample
 */
class WildvinePummelerTest : FunSpec({

    fun createDriverAndRegistry(): Pair<GameTestDriver, CardRegistry> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WildvinePummeler)

        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(WildvinePummeler)

        return driver to registry
    }

    test("no colored permanents - no cost reduction") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Lands are colorless permanents, no reduction
        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 6
        effectiveCost.cmc shouldBe 7 // {6}{G}
    }

    test("one colored permanent - reduces by 1") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a green creature on the battlefield (1 color = green)
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 5 // 6 - 1
        effectiveCost.cmc shouldBe 6 // {5}{G}
    }

    test("multiple permanents of same color - only counts once") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two green creatures = still only 1 color
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        driver.putCreatureOnBattlefield(activePlayer, "Force of Nature")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 5 // 6 - 1 (green counted once)
        effectiveCost.cmc shouldBe 6
    }

    test("three different colors - reduces by 3") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Green + Red + White = 3 colors
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")   // Green
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")      // Red
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")    // White

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 3 // 6 - 3
        effectiveCost.cmc shouldBe 4 // {3}{G}
    }

    test("five colors - reduces by 5") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // All 5 colors
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")   // Green
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")      // Red
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")    // White
        driver.putCreatureOnBattlefield(activePlayer, "Black Creature")    // Black
        driver.putCreatureOnBattlefield(activePlayer, "Island Walker")     // Blue

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 1 // 6 - 5
        effectiveCost.cmc shouldBe 2 // {1}{G}
    }

    test("opponent's colored permanents do not count") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has colored permanents, active player has none
        driver.putCreatureOnBattlefield(driver.player2, "Goblin Guide")
        driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 6 // No reduction
        effectiveCost.cmc shouldBe 7
    }

    test("colorless permanents do not contribute to color count") {
        val (driver, registry) = createDriverAndRegistry()
        val calculator = CostCalculator(registry)

        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Artifact creature is colorless
        driver.putCreatureOnBattlefield(activePlayer, "Artifact Creature")

        val pummelerDef = registry.requireCard("Wildvine Pummeler")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, pummelerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 6 // No reduction from colorless
        effectiveCost.cmc shouldBe 7
    }
})
