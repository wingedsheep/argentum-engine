package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.StagBeetle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Stag Beetle.
 *
 * Stag Beetle: {3}{G}{G}
 * Creature — Insect
 * 0/0
 * Stag Beetle enters the battlefield with X +1/+1 counters on it,
 * where X is the number of other creatures on the battlefield.
 */
class StagBeetleTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Stag Beetle enters with counters equal to number of other creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 3 creatures on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")

        // Cast Stag Beetle through the stack so replacement effect fires
        val beetle = driver.putCardInHand(activePlayer, "Stag Beetle")
        driver.giveMana(activePlayer, Color.GREEN, 5)

        val castResult = driver.castSpell(activePlayer, beetle)
        castResult.isSuccess shouldBe true

        // Resolve - beetle enters the battlefield
        driver.bothPass()

        // Stag Beetle should be a 3/3 (0/0 base + 3 counters from 3 other creatures)
        projector.getProjectedPower(driver.state, beetle) shouldBe 3
        projector.getProjectedToughness(driver.state, beetle) shouldBe 3
    }

    test("Stag Beetle enters as 0/0 and dies when no other creatures exist") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No other creatures on the battlefield
        val beetle = driver.putCardInHand(activePlayer, "Stag Beetle")
        driver.giveMana(activePlayer, Color.GREEN, 5)

        driver.castSpell(activePlayer, beetle)
        driver.bothPass()

        // With 0 counters, it's a 0/0 and should die to SBAs
        val onBattlefield = driver.state.getBattlefield().contains(beetle)
        onBattlefield shouldBe false
    }

    test("Stag Beetle counts opponent creatures too") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != activePlayer }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 1 creature on each side = 2 total
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val beetle = driver.putCardInHand(activePlayer, "Stag Beetle")
        driver.giveMana(activePlayer, Color.GREEN, 5)

        driver.castSpell(activePlayer, beetle)
        driver.bothPass()

        // 2 other creatures → 2/2
        projector.getProjectedPower(driver.state, beetle) shouldBe 2
        projector.getProjectedToughness(driver.state, beetle) shouldBe 2
    }
})
