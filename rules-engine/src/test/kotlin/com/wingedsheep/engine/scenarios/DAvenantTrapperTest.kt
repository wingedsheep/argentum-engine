package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.DAvenantTrapper
import com.wingedsheep.mtg.sets.definitions.mrd.cards.GildedLotus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for D'Avenant Trapper:
 * {2}{W} - Creature — Human Archer (3/2)
 * Whenever you cast a historic spell, tap target creature an opponent controls.
 *
 * ## Covered Scenarios
 * - Casting an artifact (historic) triggers the tap ability
 * - Casting a non-historic spell does not trigger the ability
 */
class DAvenantTrapperTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DAvenantTrapper)
        driver.registerCard(GildedLotus)
        return driver
    }

    test("casting an artifact triggers tap of opponent creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 30, "Forest" to 30)
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put D'Avenant Trapper on battlefield for P1
        driver.putCreatureOnBattlefield(p1, "D'Avenant Trapper")

        // Put a creature on P2's battlefield to tap
        val opponentCreature = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        // Cast Gilded Lotus (artifact = historic)
        val lotus = driver.putCardInHand(p1, "Gilded Lotus")
        driver.giveMana(p1, Color.WHITE, 5)

        driver.castSpell(p1, lotus)
        driver.bothPass() // Resolve Gilded Lotus

        // Historic trigger should be on stack — select target
        if (driver.stackSize > 0) {
            driver.submitTargetSelection(p1, listOf(opponentCreature))
            driver.bothPass() // Resolve trigger
        }

        // Opponent's creature should be tapped
        driver.state.getEntity(opponentCreature)?.has<TappedComponent>() shouldBe true
    }

    test("casting a non-historic spell does not trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 30, "Forest" to 30)
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put D'Avenant Trapper on battlefield for P1
        driver.putCreatureOnBattlefield(p1, "D'Avenant Trapper")

        // Put a creature on P2's battlefield
        val opponentCreature = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        // Cast a non-historic spell (Grizzly Bears is a non-legendary creature)
        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.castSpell(p1, bears)
        driver.bothPass() // Resolve the spell

        // No trigger should have fired — opponent's creature stays untapped
        driver.state.getEntity(opponentCreature)?.has<TappedComponent>() shouldBe false
    }
})
