package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BonnyPallClearcutter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bonny Pall, Clearcutter (OTJ) — {3}{G}{U}{U} Legendary Giant Scout 6/5.
 *
 *  - Reach.
 *  - When Bonny Pall enters, create Beau, a legendary blue Ox with a characteristic-defining
 *    "power and toughness each equal to the number of lands you control".
 *  - Whenever you attack, draw a card, then you may put a land card from your hand or graveyard
 *    onto the battlefield.
 */
class BonnyPallClearcutterScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BonnyPallClearcutter)
        return driver
    }

    test("ETB creates Beau, a legendary blue Ox, with P/T equal to lands you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Three lands on the battlefield before Bonny Pall enters.
        repeat(3) { driver.putLandOnBattlefield(player, "Forest") }

        val bonny = driver.putCardInHand(player, "Bonny Pall, Clearcutter")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.BLUE, 2)
        driver.giveColorlessMana(player, 3)
        driver.castSpell(player, bonny)
        // Drain priority: Bonny Pall resolves → its ETB trigger goes on the stack → resolves → Beau.
        repeat(6) { if (driver.findPermanent(player, "Beau") == null) driver.bothPass() }

        val beau = driver.findPermanent(player, "Beau")
        beau shouldNotBe null

        val projected = projector.project(driver.state)
        // 3 lands → Beau is 3/3.
        projected.getPower(beau!!) shouldBe 3
        projected.getToughness(beau) shouldBe 3
    }

    test("Beau's power and toughness track lands continuously as the count changes") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(2) { driver.putLandOnBattlefield(player, "Forest") }

        val bonny = driver.putCardInHand(player, "Bonny Pall, Clearcutter")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.BLUE, 2)
        driver.giveColorlessMana(player, 3)
        driver.castSpell(player, bonny)
        repeat(6) { if (driver.findPermanent(player, "Beau") == null) driver.bothPass() }

        val beau = driver.findPermanent(player, "Beau")!!
        projector.project(driver.state).getPower(beau) shouldBe 2

        // Add two more lands — Beau grows to 4/4 (continuous CDA, not snapshot at creation).
        repeat(2) { driver.putLandOnBattlefield(player, "Forest") }
        val grown = projector.project(driver.state)
        grown.getPower(beau) shouldBe 4
        grown.getToughness(beau) shouldBe 4
    }

    test("attack trigger draws a card and may put a land from hand onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bonny = driver.putCreatureOnBattlefield(player, "Bonny Pall, Clearcutter")
        driver.removeSummoningSickness(bonny)
        // A land waiting in hand to be put onto the battlefield by the trigger.
        driver.putCardInHand(player, "Forest")

        val handBefore = driver.getHandSize(player)
        val landsBefore = driver.getLands(player).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bonny), opponent)
        driver.bothPass() // resolve the "whenever you attack" trigger: draw, then choose a land

        // Drew a card (the trigger's draw resolves before the optional land-put selection).
        driver.getHandSize(player) shouldBe handBefore + 1

        // The optional land-put selection from hand/graveyard is presented.
        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        val landOption = decision.options.first { driver.getCardName(it) == "Forest" }
        driver.submitCardSelection(player, listOf(landOption))

        // The chosen land entered the battlefield.
        driver.getLands(player).size shouldBe landsBefore + 1
    }

    test("attack trigger may put a land from the graveyard onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bonny = driver.putCreatureOnBattlefield(player, "Bonny Pall, Clearcutter")
        driver.removeSummoningSickness(bonny)
        driver.putCardInGraveyard(player, "Forest")

        val landsBefore = driver.getLands(player).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bonny), opponent)
        driver.bothPass()

        val decision = driver.pendingDecision as SelectCardsDecision
        // The graveyard Forest is an eligible option (multi-zone gather).
        decision.options.mapNotNull { driver.getCardName(it) } shouldContain "Forest"
        val landOption = decision.options.first { driver.getCardName(it) == "Forest" }
        driver.submitCardSelection(player, listOf(landOption))

        driver.getLands(player).size shouldBe landsBefore + 1
    }
})
