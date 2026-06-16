package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.MaraudingSphinx
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.mechanics.layers.StateProjector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Marauding Sphinx — {3}{U}{U} 3/5 Creature — Sphinx Rogue
 *
 * "Flying, vigilance, ward {2}
 *  Whenever you commit a crime, surveil 2. This ability triggers only once each turn."
 */
class MaraudingSphinxScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MaraudingSphinx)
        return driver
    }

    test("has flying and vigilance") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val sphinx = driver.putCreatureOnBattlefield(me, "Marauding Sphinx")
        val projected = projector.project(driver.state)
        projected.hasKeyword(sphinx, Keyword.FLYING) shouldBe true
        projected.hasKeyword(sphinx, Keyword.VIGILANCE) shouldBe true
    }

    test("committing a crime surveils 2, but only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Marauding Sphinx")

        // Seed a known library top so surveil presents two cards.
        val top2 = driver.putCardOnTopOfLibrary(me, "Island")
        val top1 = driver.putCardOnTopOfLibrary(me, "Mountain")

        // Commit a crime: cast Lightning Bolt targeting the opponent.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime event -> surveil trigger on stack
        driver.bothPass() // resolve surveil trigger -> pauses for the surveil decision

        // Surveil 2 pauses for the keep/graveyard choice.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val select = driver.pendingDecision as SelectCardsDecision
        select.options.size shouldBe 2

        // Mill the top card; keep the second on top.
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(top1)))
        driver.isPaused shouldBe true
        val reorder = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(me, reorder.cards)
        driver.isPaused shouldBe false
        driver.getGraveyard(me).contains(top1) shouldBe true

        // Commit a SECOND crime the same turn — the ability triggers only once each turn,
        // so no second surveil decision should appear.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe false
        // top2 is still the library top: no second surveil moved it.
        driver.state.getLibrary(me).first() shouldBe top2
    }
})
