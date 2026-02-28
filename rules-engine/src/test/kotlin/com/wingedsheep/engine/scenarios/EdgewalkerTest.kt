package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ReduceSpellColoredCostBySubtype
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Edgewalker.
 *
 * Edgewalker: {1}{W}{B}
 * Creature — Human Cleric
 * 2/2
 * Cleric spells you cast cost {W}{B} less to cast.
 * This effect reduces only the amount of colored mana you pay.
 */
class EdgewalkerTest : FunSpec({

    val Edgewalker = card("Edgewalker") {
        manaCost = "{1}{W}{B}"
        typeLine = "Creature — Human Cleric"
        power = 2
        toughness = 2

        staticAbility {
            ability = ReduceSpellColoredCostBySubtype(
                subtype = "Cleric",
                manaReduction = "{W}{B}"
            )
        }
    }

    // A Cleric that costs {1}{W}{B} — both W and B can be reduced
    val TestWBCleric = card("Test WB Cleric") {
        manaCost = "{1}{W}{B}"
        typeLine = "Creature — Human Cleric"
        power = 2
        toughness = 2
    }

    // A Cleric that costs {W} only — only W can be reduced
    val TestWCleric = card("Test W Cleric") {
        manaCost = "{W}"
        typeLine = "Creature — Cleric"
        power = 1
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Edgewalker)
        driver.registerCard(TestWBCleric)
        driver.registerCard(TestWCleric)
        return driver
    }

    test("Cleric spell with {W} and {B} has both reduced") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(Edgewalker)
        registry.register(TestWBCleric)

        val calculator = CostCalculator(registry)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")

        // Test WB Cleric costs {1}{W}{B}, with Edgewalker it should cost {1}
        val wbClericDef = registry.requireCard("Test WB Cleric")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, wbClericDef, activePlayer)
        effectiveCost.cmc shouldBe 1
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.colorCount shouldBe emptyMap()
    }

    test("Cleric spell with only {W} has only {W} reduced") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(Edgewalker)
        registry.register(TestWCleric)

        val calculator = CostCalculator(registry)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")

        // Test W Cleric costs {W}, with Edgewalker it should cost {0} (free)
        val wClericDef = registry.requireCard("Test W Cleric")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, wClericDef, activePlayer)
        effectiveCost.cmc shouldBe 0
        effectiveCost.symbols shouldBe emptyList()
    }

    test("Test Cleric {1}{W} has {W} reduced to {1}") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(Edgewalker)

        val calculator = CostCalculator(registry)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")

        // Test Cleric costs {1}{W} — Edgewalker reduces {W}{B}, but only {W} is present
        // So it becomes {1}
        val testClericDef = registry.requireCard("Test Cleric")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, testClericDef, activePlayer)
        effectiveCost.cmc shouldBe 1
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.colorCount shouldBe emptyMap()
    }

    test("does not reduce non-Cleric spells") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(Edgewalker)

        val calculator = CostCalculator(registry)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10, "Forest" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")

        // Grizzly Bears costs {1}{G} — not a Cleric, should not be reduced
        val bearsDef = registry.requireCard("Grizzly Bears")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bearsDef, activePlayer)
        effectiveCost.cmc shouldBe 2
        effectiveCost.genericAmount shouldBe 1
    }

    test("multiple Edgewalkers stack cost reduction") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(Edgewalker)
        registry.register(TestWBCleric)

        val calculator = CostCalculator(registry)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")

        // Test WB Cleric costs {1}{W}{B}
        // Two Edgewalkers reduce {W}{B} + {W}{B}, but spell only has one {W} and one {B}
        // So it still becomes {1} (can't reduce more colored mana than exists)
        val wbClericDef = registry.requireCard("Test WB Cleric")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, wbClericDef, activePlayer)
        effectiveCost.cmc shouldBe 1
        effectiveCost.genericAmount shouldBe 1
    }

    test("Edgewalker reduces its own casting cost for another Edgewalker") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(Edgewalker)

        val calculator = CostCalculator(registry)
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(activePlayer, "Edgewalker")

        // Another Edgewalker costs {1}{W}{B}, reduced by {W}{B} to just {1}
        val edgewalkerDef = registry.requireCard("Edgewalker")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, edgewalkerDef, activePlayer)
        effectiveCost.cmc shouldBe 1
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.colorCount shouldBe emptyMap()
    }
})
