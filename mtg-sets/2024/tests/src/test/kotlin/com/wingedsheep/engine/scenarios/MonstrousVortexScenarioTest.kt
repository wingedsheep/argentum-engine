package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.MonstrousVortex
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Monstrous Vortex {3}{G} — "Whenever you cast a creature spell with power 5 or greater,
 * discover X, where X is that spell's mana value."
 */
class MonstrousVortexScenarioTest : FunSpec({

    // Power 5, mana value 5 — triggers the Vortex.
    val brute = card("Test Brute") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Beast"
        power = 5
        toughness = 5
    }
    // Power 4, mana value 5 — below the power threshold.
    val tallFour = card("Test Tall Four") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 6
    }
    // A mana-value-5 nonland: discovered only when X >= 5.
    val relic = card("Test Relic") {
        manaCost = "{5}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    fun setup(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(brute)
        driver.registerCard(tallFour)
        driver.registerCard(relic)
        driver.registerCard(MonstrousVortex)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Monstrous Vortex")
        driver.putCardOnTopOfLibrary(me, "Test Relic")
        return driver to me
    }

    test("casting a power-5 creature spell discovers its mana value") {
        val (driver, me) = setup()
        val brute = driver.putCardInHand(me, "Test Brute")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, brute)

        // The Vortex trigger is above the creature spell.
        driver.stackSize shouldBe 2
        driver.bothPass()

        // Discover 5 found the mana-value-5 relic → cast-or-hand decision.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    test("a power-4 creature spell does not trigger") {
        val (driver, me) = setup()
        val four = driver.putCardInHand(me, "Test Tall Four")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, four)

        driver.stackSize shouldBe 1
    }
})
