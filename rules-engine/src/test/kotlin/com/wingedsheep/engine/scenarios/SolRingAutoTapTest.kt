package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.SolRing
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sol Ring's mana ability is "{T}: Add {C}{C}" — two colorless from a single tap. Regression net
 * for the auto-tapper, which previously dropped one of the two mana when a multi-mana colorless
 * source paid a generic/{C} cost, so a lone Sol Ring couldn't cover {2} (or {C}{C}) by itself.
 */
class SolRingAutoTapTest : FunSpec({

    val genericTwo = card("Generic Two") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    val genericOne = card("Generic One") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    val colorlessTwo = card("Colorless Two") {
        manaCost = "{C}{C}"
        typeLine = "Artifact"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SolRing, genericTwo, genericOne, colorlessTwo))
        return driver
    }

    fun setup(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return player
    }

    test("a single Sol Ring pays {2}") {
        val driver = createDriver()
        val player = setup(driver)
        driver.putPermanentOnBattlefield(player, "Sol Ring")

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, player, ManaCost.parse("{2}")) shouldBe true
    }

    test("a single Sol Ring does NOT pay {3} (only two mana)") {
        val driver = createDriver()
        val player = setup(driver)
        driver.putPermanentOnBattlefield(player, "Sol Ring")

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, player, ManaCost.parse("{3}")) shouldBe false
    }

    test("a single Sol Ring pays {C}{C}") {
        val driver = createDriver()
        val player = setup(driver)
        driver.putPermanentOnBattlefield(player, "Sol Ring")

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, player, ManaCost.parse("{C}{C}")) shouldBe true
    }

    test("auto-pay casts a {2} spell tapping only the Sol Ring") {
        val driver = createDriver()
        val player = setup(driver)
        val ring = driver.putPermanentOnBattlefield(player, "Sol Ring")
        val spell = driver.putCardInHand(player, "Generic Two")

        val result = driver.castSpell(player, spell)
        result.isSuccess shouldBe true
        driver.isTapped(ring) shouldBe true
    }

    test("casting {1} with a Sol Ring floats the leftover colorless") {
        val driver = createDriver()
        val player = setup(driver)
        driver.putPermanentOnBattlefield(player, "Sol Ring")
        val spell = driver.putCardInHand(player, "Generic One")

        val result = driver.castSpell(player, spell)
        result.isSuccess shouldBe true
        // Sol Ring produced {C}{C}; {1} paid the spell, one {C} should remain floating.
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>()
        (pool?.colorless ?: 0) shouldBe 1
    }
})
