package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.NoggleTheMind
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Tests for Noggle the Mind
 * {1}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature loses all abilities and is a colorless Noggle
 * with base power and toughness 1/1. (It loses all colors and all other creature types.)
 */
class NoggleTheMindTest : FunSpec({

    val TrampleCreature = CardDefinition.creature(
        name = "Trampling Beast",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5,
        toughness = 4,
        keywords = setOf(Keyword.TRAMPLE)
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TrampleCreature, NoggleTheMind))
        return driver
    }

    test("sets base power and toughness to 1/1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Trampling Beast")

        val aura = driver.putCardInHand(activePlayer, "Noggle the Mind")
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveColorlessMana(activePlayer, 1)
        driver.castSpell(activePlayer, aura, listOf(creature))
        driver.bothPass()

        projector.getProjectedPower(driver.state, creature) shouldBe 1
        projector.getProjectedToughness(driver.state, creature) shouldBe 1
    }

    test("removes trample") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Trampling Beast")

        val aura = driver.putCardInHand(activePlayer, "Noggle the Mind")
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveColorlessMana(activePlayer, 1)
        driver.castSpell(activePlayer, aura, listOf(creature))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.hasKeyword(creature, Keyword.TRAMPLE) shouldBe false
        projected.hasLostAllAbilities(creature) shouldBe true
    }

    test("becomes a colorless Noggle, losing original types and colors") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Trampling Beast")

        val aura = driver.putCardInHand(activePlayer, "Noggle the Mind")
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveColorlessMana(activePlayer, 1)
        driver.castSpell(activePlayer, aura, listOf(creature))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.hasSubtype(creature, "Noggle") shouldBe true
        projected.hasSubtype(creature, "Beast") shouldBe false
        projected.getColors(creature).shouldBeEmpty() // colorless
    }
})
