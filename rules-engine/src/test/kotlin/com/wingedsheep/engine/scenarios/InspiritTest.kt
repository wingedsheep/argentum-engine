package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Inspirit.
 *
 * Inspirit: {2}{W}
 * Instant
 * Untap target creature. It gets +2/+4 until end of turn.
 */
class InspiritTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Inspirit untaps a tapped creature and gives it +2/+4") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature and tap it
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.tapPermanent(bears)
        driver.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

        // Cast Inspirit targeting the tapped creature
        val inspirit = driver.putCardInHand(activePlayer, "Inspirit")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        val castResult = driver.castSpell(activePlayer, inspirit, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Creature should now be untapped
        driver.state.getEntity(bears)?.has<TappedComponent>() shouldBe false

        // Creature should have +2/+4 (2/2 -> 4/6)
        projector.getProjectedPower(driver.state, bears) shouldBe 4
        projector.getProjectedToughness(driver.state, bears) shouldBe 6
    }

    test("Inspirit works on an already untapped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature (already untapped)
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.state.getEntity(bears)?.has<TappedComponent>() shouldBe false

        // Cast Inspirit
        val inspirit = driver.putCardInHand(activePlayer, "Inspirit")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        driver.castSpell(activePlayer, inspirit, listOf(bears))
        driver.bothPass()

        // Still untapped and gets the buff
        driver.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
        projector.getProjectedPower(driver.state, bears) shouldBe 4
        projector.getProjectedToughness(driver.state, bears) shouldBe 6
    }
})
