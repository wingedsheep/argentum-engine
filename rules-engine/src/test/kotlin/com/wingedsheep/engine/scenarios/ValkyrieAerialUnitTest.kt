package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.ValkyrieAerialUnit
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Valkyrie Aerial Unit — {5}{U}{U} 5/4 Artifact Creature — Construct
 *
 * "Affinity for artifacts
 *  Flying
 *  When this creature enters, surveil 2."
 */
class ValkyrieAerialUnitTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ValkyrieAerialUnit)
        return driver
    }

    test("has flying and its ETB surveils 2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!

        // Seed a known library top so surveil presents two cards.
        val top2 = driver.putCardOnTopOfLibrary(me, "Island")
        val top1 = driver.putCardOnTopOfLibrary(me, "Island")

        val valkyrie = driver.putCardInHand(me, "Valkyrie Aerial Unit")
        driver.giveColorlessMana(me, 5)
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.BLUE, 2)
        driver.castSpell(me, valkyrie)
        driver.bothPass() // resolve creature -> ETB surveil trigger on stack
        driver.bothPass() // resolve trigger -> pause for surveil decision

        val select = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.options.size shouldBe 2

        // Mill the top card; keep the second.
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(top1)))
        val reorder = driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        driver.submitOrderedResponse(me, reorder.cards)

        driver.getGraveyard(me).contains(top1) shouldBe true
        driver.state.getLibrary(me).first() shouldBe top2

        // Flying is present on the resolved creature.
        val unit = driver.findPermanent(me, "Valkyrie Aerial Unit")!!
        projector.project(driver.state).hasKeyword(unit, Keyword.FLYING) shouldBe true
    }

    test("affinity for artifacts reduces the generic cost per artifact controlled") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(ValkyrieAerialUnit)
        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        // No artifacts -> full {5} generic.
        val costBefore = calculator.calculateEffectiveCost(
            driver.state, registry.requireCard("Valkyrie Aerial Unit"), me
        )
        costBefore.genericAmount shouldBe 5

        // Control two artifacts -> {5} - 2 = {3} generic.
        driver.putPermanentOnBattlefield(me, "Bonesplitter")
        driver.putPermanentOnBattlefield(me, "Leonin Scimitar")
        val costAfter = calculator.calculateEffectiveCost(
            driver.state, registry.requireCard("Valkyrie Aerial Unit"), me
        )
        costAfter.genericAmount shouldBe 3
    }
})
