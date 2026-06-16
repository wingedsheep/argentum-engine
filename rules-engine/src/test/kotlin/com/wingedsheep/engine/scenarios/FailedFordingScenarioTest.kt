package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.FailedFording
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Failed Fording (OTJ #47) — {1}{U} Instant.
 *
 *   "Return target nonland permanent to its owner's hand. If you control a Desert, surveil 1."
 *
 * Verifies the bounce always happens, and the surveil only triggers when the controller controls
 * a Desert (a one-shot resolution-time `ConditionalEffect`, not an intervening-if trigger).
 */
class FailedFordingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(FailedFording))
        return driver
    }

    test("bounces the target; no surveil without a Desert") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val libTop = driver.putCardOnTopOfLibrary(me, "Island")

        val card = driver.putCardInHand(me, "Failed Fording")
        driver.giveMana(me, Color.BLUE, 2)
        driver.castSpell(me, card, targets = listOf(bears))
        driver.bothPass()

        // No Desert -> no surveil decision; the spell resolves fully.
        driver.isPaused shouldBe false
        // Grizzly Bears returned to its owner's hand.
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(opp, Zone.BATTLEFIELD))
            .contains(bears) shouldBe false
        driver.getHand(opp).any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        } shouldBe true
        // Library top untouched (no surveil).
        driver.state.getLibrary(me).first() shouldBe libTop
    }

    test("with a Desert, also surveils 1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Control a Desert (Desert is itself a land with subtype Desert).
        driver.putLandOnBattlefield(me, "Desert")

        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val surveilTop = driver.putCardOnTopOfLibrary(me, "Island")

        val card = driver.putCardInHand(me, "Failed Fording")
        driver.giveMana(me, Color.BLUE, 2)
        driver.castSpell(me, card, targets = listOf(bears))
        driver.bothPass()

        // Surveil 1 pauses for the keep/graveyard choice.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val select = driver.pendingDecision as SelectCardsDecision
        select.options.size shouldBe 1

        // Put the looked-at card into the graveyard.
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(surveilTop)))
        // No remainder to reorder for surveil 1 -> resolution completes (handle a possible
        // trivial reorder decision defensively).
        if (driver.isPaused && driver.pendingDecision is ReorderLibraryDecision) {
            val reorder = driver.pendingDecision as ReorderLibraryDecision
            driver.submitOrderedResponse(me, reorder.cards)
        }
        driver.isPaused shouldBe false

        // The bounce happened and the surveil milled the top card.
        driver.getHand(opp).any {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        } shouldBe true
        driver.getGraveyard(me).contains(surveilTop) shouldBe true
    }
})
