package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Stormwing Entity (M21 #73).
 *
 * {3}{U}{U} Creature — Elemental
 * "This spell costs {2}{U} less to cast if you've cast an instant or sorcery spell this turn."
 *
 * The discount is two gated self-cast modifiers (generic {2} and colored {U}); this proves both
 * halves land together, that only instants and sorceries enable them, and that several instants
 * don't stack the discount (2020-06-23 ruling).
 */
class StormwingEntityScenarioTest : FunSpec({

    val TestInstant = card("Test Gain One Instant") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }
    val TestSorcery = card("Test Gain One Sorcery") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    fun createRegistry(): CardRegistry = CardRegistry().apply {
        register(TestCards.all)
        register(TestInstant)
        register(TestSorcery)
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TestInstant)
        driver.registerCard(TestSorcery)
        return driver
    }

    fun cast(driver: GameTestDriver, playerId: EntityId, cardName: String, color: Color, amount: Int) {
        val spell = driver.putCardInHand(playerId, cardName)
        driver.giveMana(playerId, color, amount)
        driver.castSpell(playerId, spell).error shouldBe null
        driver.bothPass()
    }

    fun setup(): Triple<GameTestDriver, CostCalculator, EntityId> {
        val registry = createRegistry()
        val calculator = CostCalculator(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, calculator, you)
    }

    test("full {3}{U}{U} when no instant or sorcery was cast this turn") {
        val (driver, calculator, you) = setup()
        val entity = createRegistry().requireCard("Stormwing Entity")

        val cost = calculator.calculateEffectiveCost(driver.state, entity, you)
        cost.genericAmount shouldBe 3
        cost.cmc shouldBe 5
    }

    test("costs {1}{U} after casting an instant this turn") {
        val (driver, calculator, you) = setup()
        val entity = createRegistry().requireCard("Stormwing Entity")

        cast(driver, you, "Test Gain One Instant", Color.WHITE, 1)

        val cost = calculator.calculateEffectiveCost(driver.state, entity, you)
        cost.genericAmount shouldBe 1
        cost.cmc shouldBe 2
    }

    test("a sorcery also enables it, and two spells don't stack the discount") {
        val (driver, calculator, you) = setup()
        val entity = createRegistry().requireCard("Stormwing Entity")

        cast(driver, you, "Test Gain One Sorcery", Color.WHITE, 1)
        cast(driver, you, "Test Gain One Instant", Color.WHITE, 1)

        val cost = calculator.calculateEffectiveCost(driver.state, entity, you)
        cost.genericAmount shouldBe 1
        cost.cmc shouldBe 2
    }

    test("casting a creature spell does not enable the discount") {
        val (driver, calculator, you) = setup()
        val entity = createRegistry().requireCard("Stormwing Entity")

        cast(driver, you, "Grizzly Bears", Color.GREEN, 2)

        val cost = calculator.calculateEffectiveCost(driver.state, entity, you)
        cost.genericAmount shouldBe 3
        cost.cmc shouldBe 5
    }
})
