package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.GlorySeeker
import com.wingedsheep.mtg.sets.definitions.scourge.cards.DaruWarchief
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Daru Warchief.
 *
 * Daru Warchief: {2}{W}{W}
 * Creature — Human Soldier
 * 1/1
 * Soldier spells you cast cost {1} less to cast.
 * Soldier creatures you control get +1/+2.
 */
class DaruWarchiefTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DaruWarchief)
        driver.registerCard(GlorySeeker)
        return driver
    }

    test("Soldier creatures you control get +1/+2") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")

        val projected = projector.project(driver.state)
        projected.getPower(soldier) shouldBe 3  // 2 + 1
        projected.getToughness(soldier) shouldBe 4  // 2 + 2
    }

    test("non-Soldier creatures do not get the bonus") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Forest" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val projected = projector.project(driver.state)
        projected.getPower(bears) shouldBe 2
        projected.getToughness(bears) shouldBe 2
    }

    test("Daru Warchief itself gets +1/+2 (it is a Soldier)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warchief = driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")

        val projected = projector.project(driver.state)
        projected.getPower(warchief) shouldBe 2  // 1 + 1
        projected.getToughness(warchief) shouldBe 3  // 1 + 2
    }

    test("Soldier spells cost 1 less to cast") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(DaruWarchief)
        registry.register(GlorySeeker)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")

        // Glory Seeker costs {1}{W} = 1 generic + 1 white
        // With Warchief, generic reduced by 1 → just {W}
        val glorySeekerDef = registry.requireCard("Glory Seeker")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, glorySeekerDef, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 1  // Just {W}
    }

    test("cost reduction does not apply to non-Soldier spells") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(DaruWarchief)
        registry.register(GlorySeeker)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Forest" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")

        // Grizzly Bears costs {1}{G} — not a Soldier, should NOT be reduced
        val bears = registry.requireCard("Grizzly Bears")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, bears, activePlayer)
        effectiveCost.genericAmount shouldBe 1
        effectiveCost.cmc shouldBe 2  // Still {1}{G}
    }

    test("multiple Warchiefs stack both cost reduction and stat bonus") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(DaruWarchief)
        registry.register(GlorySeeker)

        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warchief1 = driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")
        val warchief2 = driver.putCreatureOnBattlefield(activePlayer, "Daru Warchief")
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")

        // Stats: each Warchief gives +1/+2 to all Soldiers you control
        val projected = projector.project(driver.state)
        projected.getPower(soldier) shouldBe 4    // 2 + 1 + 1
        projected.getToughness(soldier) shouldBe 6  // 2 + 2 + 2

        // Each Warchief also buffs itself and the other
        projected.getPower(warchief1) shouldBe 3    // 1 + 1 + 1
        projected.getToughness(warchief1) shouldBe 5  // 1 + 2 + 2

        // Cost reduction: Daru Warchief costs {2}{W}{W} = 2 generic
        // With two Warchiefs, reduction is 2 → just {W}{W}
        val warchiefDef = registry.requireCard("Daru Warchief")
        val effectiveCost = calculator.calculateEffectiveCost(driver.state, warchiefDef, activePlayer)
        effectiveCost.genericAmount shouldBe 0
        effectiveCost.cmc shouldBe 2  // Just {W}{W}
    }
})
