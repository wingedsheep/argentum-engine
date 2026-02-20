package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.CarrionFeeder
import com.wingedsheep.mtg.sets.definitions.scourge.cards.UndeadWarchief
import com.wingedsheep.mtg.sets.definitions.scourge.cards.VengefulDead
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Undead Warchief.
 *
 * Undead Warchief: {2}{B}{B}
 * Creature — Zombie
 * 1/1
 * Zombie spells you cast cost {1} less to cast.
 * Zombie creatures you control get +2/+1.
 */
class UndeadWarchiefTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UndeadWarchief)
        driver.registerCard(CarrionFeeder)
        driver.registerCard(VengefulDead)
        return driver
    }

    test("Zombie creatures you control get +2/+1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")
        val zombie = driver.putCreatureOnBattlefield(activePlayer, "Carrion Feeder")

        val projected = projector.project(driver.state)
        projected.getPower(zombie) shouldBe 3  // 1 + 2
        projected.getToughness(zombie) shouldBe 2  // 1 + 1
    }

    test("non-Zombie creatures do not get the bonus") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 10, "Forest" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val projected = projector.project(driver.state)
        projected.getPower(bears) shouldBe 2
        projected.getToughness(bears) shouldBe 2
    }

    test("Undead Warchief itself gets +2/+1 (it is a Zombie)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warchief = driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")

        val projected = projector.project(driver.state)
        projected.getPower(warchief) shouldBe 3  // 1 + 2
        projected.getToughness(warchief) shouldBe 2  // 1 + 1
    }

    test("Zombie spells cost 1 less to cast") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(UndeadWarchief)
        registry.register(CarrionFeeder)
        registry.register(VengefulDead)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")

        // Vengeful Dead costs {3}{B} = 3 generic + 1 black
        // With Warchief, generic reduced by 1 → {2}{B}
        val vengefulDeadDef = registry.requireCard("Vengeful Dead")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, vengefulDeadDef, activePlayer)
        effectiveCost.genericAmount shouldBe 2
        effectiveCost.cmc shouldBe 3  // {2}{B}
    }

    test("cost reduction does not apply to non-Zombie spells") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(UndeadWarchief)
        registry.register(CarrionFeeder)
        registry.register(VengefulDead)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 10, "Forest" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")

        // Grizzly Bears costs {1}{G} — not a Zombie, should NOT be reduced
        val bears = registry.requireCard("Grizzly Bears")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bears, activePlayer)
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.cmc shouldBe 2  // Still {1}{G}
    }

    test("multiple Warchiefs stack both cost reduction and stat bonus") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(UndeadWarchief)
        registry.register(CarrionFeeder)
        registry.register(VengefulDead)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warchief1 = driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")
        val warchief2 = driver.putCreatureOnBattlefield(activePlayer, "Undead Warchief")
        val zombie = driver.putCreatureOnBattlefield(activePlayer, "Carrion Feeder")

        // Stats: each Warchief gives +2/+1 to all Zombies you control
        val projected = projector.project(driver.state)
        projected.getPower(zombie) shouldBe 5    // 1 + 2 + 2
        projected.getToughness(zombie) shouldBe 3  // 1 + 1 + 1

        // Each Warchief also buffs itself and the other
        projected.getPower(warchief1) shouldBe 5    // 1 + 2 + 2
        projected.getToughness(warchief1) shouldBe 3  // 1 + 1 + 1

        // Cost reduction: Undead Warchief costs {2}{B}{B} = 2 generic
        // With two Warchiefs, reduction is 2 → just {B}{B}
        val warchiefDef = registry.requireCard("Undead Warchief")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, warchiefDef, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 2  // Just {B}{B}
    }
})
