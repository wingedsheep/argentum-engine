package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * End-to-end land auto-tap for monocolored hybrid ("twobrid") costs. The {2/B} pip can be paid
 * with one mana of the colour OR two generic; the solver prefers the colour (one tap) when a
 * source of it is available, otherwise reserves the generic amount.
 *
 * Modelled on a Gurmag Nightwatch ({2/B}{2/G}{2/U}) cast, using an inline test creature so this
 * stays a pure engine test independent of any registered set.
 */
class MonocolorHybridManaSolverTest : FunSpec({

    val nightwatch = card("Twobrid Watcher") {
        manaCost = "{2/B}{2/G}{2/U}"
        typeLine = "Creature — Zombie"
        power = 3
        toughness = 6
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(nightwatch))
        return driver
    }

    test("{2/B}{2/G}{2/U} is payable with one Swamp, Forest, and Island") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Swamp")
        driver.putPermanentOnBattlefield(player, "Forest")
        driver.putPermanentOnBattlefield(player, "Island")

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, player, ManaCost.parse("{2/B}{2/G}{2/U}")) shouldBe true
    }

    test("{2/B}{2/G}{2/U} is payable with six generic-only lands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(6) { driver.putPermanentOnBattlefield(player, "Mountain") }

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, player, ManaCost.parse("{2/B}{2/G}{2/U}")) shouldBe true
    }

    test("{2/B}{2/G}{2/U} is NOT payable with only five generic-only lands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(5) { driver.putPermanentOnBattlefield(player, "Mountain") }

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, player, ManaCost.parse("{2/B}{2/G}{2/U}")) shouldBe false
    }

    test("auto-pay casts the twobrid creature, tapping one land per colour") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val swamp = driver.putPermanentOnBattlefield(player, "Swamp")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        val island = driver.putPermanentOnBattlefield(player, "Island")
        val watcher = driver.putCardInHand(player, "Twobrid Watcher")

        val result = driver.castSpell(player, watcher)
        result.isSuccess shouldBe true
        driver.bothPass() // resolve

        driver.findPermanent(player, "Twobrid Watcher") shouldBe watcher
        // Each colour side was paid with one land — all three are tapped.
        driver.isTapped(swamp) shouldBe true
        driver.isTapped(forest) shouldBe true
        driver.isTapped(island) shouldBe true
    }
})
