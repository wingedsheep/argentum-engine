package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.portal.cards.WarriorsCharge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for ModifyStatsForGroupEffect (Warrior's Charge and similar cards).
 */
class ModifyStatsForGroupTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Warrior's Charge gives all your creatures +1/+1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on the battlefield
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Verify base stats
        projector.getProjectedPower(driver.state, bears) shouldBe 2
        projector.getProjectedToughness(driver.state, bears) shouldBe 2
        projector.getProjectedPower(driver.state, lions) shouldBe 1
        projector.getProjectedToughness(driver.state, lions) shouldBe 1

        // Cast Warrior's Charge
        val charge = driver.putCardInHand(activePlayer, "Warrior's Charge")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, charge)
        driver.bothPass()

        // Both creatures should now be pumped
        projector.getProjectedPower(driver.state, bears) shouldBe 3
        projector.getProjectedToughness(driver.state, bears) shouldBe 3
        projector.getProjectedPower(driver.state, lions) shouldBe 2
        projector.getProjectedToughness(driver.state, lions) shouldBe 2
    }

    test("Warrior's Charge does not affect opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on both sides
        val myBears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Warrior's Charge
        val charge = driver.putCardInHand(activePlayer, "Warrior's Charge")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, charge)
        driver.bothPass()

        // Only my creatures should be pumped
        projector.getProjectedPower(driver.state, myBears) shouldBe 3
        projector.getProjectedToughness(driver.state, myBears) shouldBe 3
        projector.getProjectedPower(driver.state, theirBears) shouldBe 2
        projector.getProjectedToughness(driver.state, theirBears) shouldBe 2
    }

    test("Warrior's Charge has no effect when no creatures are on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 20
            ),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast with no creatures - should resolve without error
        val charge = driver.putCardInHand(activePlayer, "Warrior's Charge")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, charge)
        driver.bothPass()

        // No floating effects should be created
        driver.state.floatingEffects.size shouldBe 0
    }
})
