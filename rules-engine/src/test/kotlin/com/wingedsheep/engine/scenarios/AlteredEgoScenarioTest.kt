package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.AlteredEgo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Altered Ego.
 *
 * Altered Ego {X}{2}{G}{U} — Creature — Shapeshifter 0/0
 *   This spell can't be countered.
 *   You may have this creature enter as a copy of any creature on the battlefield, except it
 *   enters with X additional +1/+1 counters on it.
 *
 * These cover the `EntersAsCopy.additionalCounters` rider specifically — that the counters ride on
 * the *copy* effect rather than on a separate enters-with-counters replacement. The three printed
 * rulings that distinguishes: X counters land on top of the copied base P/T, X = 0 is a plain copy,
 * and declining the copy also declines the counters.
 */
class AlteredEgoScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AlteredEgo)
        return driver
    }

    test("copies a creature and enters with X additional +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Elvish Warrior is a 2/3; with X = 2 the copy should be a 4/5 carrying two counters.
        val warrior = driver.putCreatureOnBattlefield(player, "Elvish Warrior")
        val alteredEgo = driver.putCardInHand(player, "Altered Ego")
        driver.giveMana(player, Color.GREEN, 3)
        driver.giveMana(player, Color.BLUE, 3)
        driver.castXSpell(player, alteredEgo, xValue = 2).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain warrior
        driver.submitCardSelection(player, listOf(warrior))

        val card = driver.state.getEntity(alteredEgo)?.get<CardComponent>()
        card shouldNotBe null
        card!!.name shouldBe "Elvish Warrior"

        val counters = driver.state.getEntity(alteredEgo)?.get<CountersComponent>()
        counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

        projector.getProjectedPower(driver.state, alteredEgo) shouldBe 4
        projector.getProjectedToughness(driver.state, alteredEgo) shouldBe 5
    }

    test("X = 0 is a plain copy with no counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warrior = driver.putCreatureOnBattlefield(player, "Elvish Warrior")
        val alteredEgo = driver.putCardInHand(player, "Altered Ego")
        driver.giveMana(player, Color.GREEN, 2)
        driver.giveMana(player, Color.BLUE, 2)
        driver.castXSpell(player, alteredEgo, xValue = 0).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(warrior))

        val counters = driver.state.getEntity(alteredEgo)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        projector.getProjectedPower(driver.state, alteredEgo) shouldBe 2
        projector.getProjectedToughness(driver.state, alteredEgo) shouldBe 3
    }

    test("declining the copy declines the counters too — it enters as a 0/0 and dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Elvish Warrior")
        val alteredEgo = driver.putCardInHand(player, "Altered Ego")
        driver.giveMana(player, Color.GREEN, 5)
        driver.giveMana(player, Color.BLUE, 5)
        driver.castXSpell(player, alteredEgo, xValue = 3).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // "You can choose not to copy anything." — the copy is optional, so an empty selection is
        // legal, and without the copy the +1/+1 counters never arrive to save the 0/0.
        driver.submitCardSelection(player, emptyList())

        driver.state.getBattlefield() shouldNotContain alteredEgo
    }
})
