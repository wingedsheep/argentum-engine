package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Foot Ninjas (TMT #147) — {4}{W/B}{W/B} 5/5 Creature — Human Ninja.
 *
 * Sneak {3}{W/B} (generic Sneak behavior is covered by SneakTest).
 * "When this creature enters, you gain 3 life."
 *
 * Verifies the ETB life gain on a normal cast (the card-specific composition).
 */
class FootNinjasTest : FunSpec({

    test("entering gains its controller 3 life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        val foot = driver.putCardInHand(player, "Foot Ninjas")
        // {4}{W/B}{W/B}: six white mana pays the four generic and both hybrid pips.
        driver.giveMana(player, Color.WHITE, 6)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSpell(player, foot).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Foot Ninjas").shouldNotBeNull()
        driver.assertLifeTotal(player, 23)
    }
})
