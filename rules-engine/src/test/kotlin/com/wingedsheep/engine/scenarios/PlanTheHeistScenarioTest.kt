package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.PlanTheHeist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Plan the Heist — {2}{U}{U} Sorcery
 *
 * "Surveil 3 if you have no cards in hand. Then draw three cards. Plot {3}{U}"
 *
 * The empty-hand check happens at resolution before the draw. Casting the spell from a
 * one-card hand leaves the hand empty as the spell resolves, so surveil 3 happens, then we
 * draw three. With another card still in hand, surveil is skipped and we only draw three.
 */
class PlanTheHeistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PlanTheHeist)
        return driver
    }

    fun emptyHand(driver: GameTestDriver, playerId: EntityId) {
        val handZone = ZoneKey(playerId, Zone.HAND)
        var state = driver.state
        driver.getHand(playerId).toList().forEach { card ->
            state = state.removeFromZone(handZone, card)
        }
        driver.replaceState(state)
    }

    test("empty hand at resolution: surveil 3, then draw three") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!

        // Clear the opening hand; only Plan the Heist will be in hand, and once it's on the
        // stack the hand is empty when the spell resolves.
        emptyHand(driver, me)

        // Three known cards on top so surveil presents exactly three.
        driver.putCardOnTopOfLibrary(me, "Island")
        driver.putCardOnTopOfLibrary(me, "Island")
        val surveilTop = driver.putCardOnTopOfLibrary(me, "Island")

        val spell = driver.putCardInHand(me, "Plan the Heist")
        driver.getHand(me).size shouldBe 1

        driver.giveMana(me, Color.BLUE, 2)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, spell).isSuccess shouldBe true
        driver.bothPass() // resolve -> hand empty -> surveil 3 pauses

        driver.isPaused shouldBe true
        val select = driver.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()
        (select as SelectCardsDecision).options.size shouldBe 3

        // Mill the top card.
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(surveilTop)))
        driver.isPaused shouldBe true
        val reorder = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(me, reorder.cards)
        driver.isPaused shouldBe false

        driver.getGraveyard(me).contains(surveilTop) shouldBe true
        // Drew three afterward.
        driver.getHand(me).size shouldBe 3
    }

    test("nonempty hand at resolution: no surveil, just draw three") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!

        // Clear the opening hand, then leave exactly one spare card in hand so the hand is
        // non-empty while Plan the Heist resolves.
        emptyHand(driver, me)
        driver.putCardInHand(me, "Island")
        val spell = driver.putCardInHand(me, "Plan the Heist")

        driver.giveMana(me, Color.BLUE, 2)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, spell).isSuccess shouldBe true
        driver.bothPass() // resolve -> hand non-empty -> no surveil, just draw

        // No surveil decision: spell resolved fully.
        driver.isPaused shouldBe false
        // Spare card (1) + drew three (3) = 4.
        driver.getHand(me).size shouldBe 4
    }
})
