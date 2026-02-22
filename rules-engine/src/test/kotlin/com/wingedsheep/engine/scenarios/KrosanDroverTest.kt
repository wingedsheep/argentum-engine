package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Krosan Drover and the general ReduceSpellCostByFilter static ability.
 *
 * Krosan Drover: {3}{G}
 * Creature — Elf
 * 2/2
 * Creature spells you cast with mana value 6 or greater cost {2} less to cast.
 */
class KrosanDroverTest : FunSpec({

    val KrosanDrover = card("Krosan Drover") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2

        staticAbility {
            ability = ReduceSpellCostByFilter(
                filter = GameObjectFilter.Creature.manaValueAtLeast(6),
                amount = 2
            )
        }
    }

    // A 6-MV creature for testing
    val HillGiant6 = card("Hill Giant 6MV") {
        manaCost = "{4}{G}{G}"
        typeLine = "Creature — Giant"
        power = 5
        toughness = 5
    }

    // A 5-MV creature (below threshold)
    val HillGiant5 = card("Hill Giant 5MV") {
        manaCost = "{3}{G}{G}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 4
    }

    // A 6-MV non-creature (sorcery)
    val BigSorcery = card("Big Sorcery") {
        manaCost = "{4}{G}{G}"
        typeLine = "Sorcery"
    }

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(KrosanDrover)
        registry.register(HillGiant6)
        registry.register(HillGiant5)
        registry.register(BigSorcery)
        return registry
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(KrosanDrover)
        driver.registerCard(HillGiant6)
        driver.registerCard(HillGiant5)
        driver.registerCard(BigSorcery)
        return driver
    }

    test("creature spell with mana value 6 or greater costs 2 less") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Krosan Drover")

        // Hill Giant 6MV costs {4}{G}{G} = 4 generic, 6 CMC total
        // With Drover, reduces by 2 generic → {2}{G}{G} = 4 CMC
        val bigCreature = registry.requireCard("Hill Giant 6MV")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bigCreature, activePlayer)
        effectiveCost.genericAmount shouldBe 2
        effectiveCost.cmc shouldBe 4
    }

    test("creature spell with mana value less than 6 is not reduced") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Krosan Drover")

        // Hill Giant 5MV costs {3}{G}{G} = 5 CMC, below threshold of 6
        val smallCreature = registry.requireCard("Hill Giant 5MV")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, smallCreature, activePlayer)
        effectiveCost.genericAmount shouldBe 3
        effectiveCost.cmc shouldBe 5
    }

    test("non-creature spell with mana value 6 or greater is not reduced") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Krosan Drover")

        // Big Sorcery costs {4}{G}{G} = 6 CMC but is not a creature
        val sorcery = registry.requireCard("Big Sorcery")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, sorcery, activePlayer)
        effectiveCost.genericAmount shouldBe 4
        effectiveCost.cmc shouldBe 6
    }

    test("multiple Drovers stack cost reduction") {
        val registry = createRegistry()
        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Krosan Drover")
        driver.putCreatureOnBattlefield(activePlayer, "Krosan Drover")

        // Hill Giant 6MV costs {4}{G}{G} = 4 generic
        // With two Drovers, reduces by 4 generic → {G}{G} = 2 CMC
        val bigCreature = registry.requireCard("Hill Giant 6MV")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bigCreature, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 2
    }
})
