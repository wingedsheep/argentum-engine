package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.VerixBladewing
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Verix Bladewing:
 * {2}{R}{R} - Legendary Creature — Dragon (4/4)
 * Kicker {3}
 * Flying
 * When Verix Bladewing enters, if it was kicked, create Karox Bladewing,
 * a legendary 4/4 red Dragon creature token with flying.
 *
 * ## Covered Scenarios
 * - Cast without kicker: no token created
 * - Cast with kicker: creates legendary 4/4 Dragon token named Karox Bladewing
 */
class VerixBladewingTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VerixBladewing)
        return driver
    }

    test("cast without kicker creates no token") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30)
        )

        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val verix = driver.putCardInHand(p1, "Verix Bladewing")
        repeat(4) { driver.putLandOnBattlefield(p1, "Mountain") }

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = verix,
                wasKicked = false,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        // Resolve spell
        driver.passPriority(p1)
        driver.passPriority(driver.player2)

        // Resolve ETB trigger if on stack (conditional should do nothing)
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Only Verix on battlefield, no token
        val creatures = driver.getCreatures(p1)
        creatures.size shouldBe 1
    }

    test("cast with kicker creates legendary Karox Bladewing token") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Forest" to 30)
        )

        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val verix = driver.putCardInHand(p1, "Verix Bladewing")
        repeat(7) { driver.putLandOnBattlefield(p1, "Mountain") } // 4 + 3 kicker

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = verix,
                wasKicked = true,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        // Resolve spell
        driver.passPriority(p1)
        driver.passPriority(driver.player2)

        // Resolve ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Two creatures on battlefield: Verix + Karox token
        val creatures = driver.getCreatures(p1)
        creatures.size shouldBe 2

        // Verify Karox Bladewing token exists and is legendary
        val karox = creatures.mapNotNull { id ->
            driver.state.getEntity(id)?.get<CardComponent>()
        }.find { it.name == "Karox Bladewing" }

        (karox != null) shouldBe true
        karox!!.typeLine.isLegendary shouldBe true
        karox.typeLine.isCreature shouldBe true
    }
})
