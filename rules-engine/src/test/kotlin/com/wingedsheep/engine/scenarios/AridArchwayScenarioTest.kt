package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AridArchway
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arid Archway (OTJ #252) — Land — Desert.
 *
 *   "This land enters tapped.
 *    When this land enters, return a land you control to its owner's hand. If another Desert was
 *    returned this way, surveil 1.
 *    {T}: Add {C}{C}."
 *
 * Verifies: enters tapped; the ETB bounce always returns the chosen land; surveil 1 fires only when
 * the returned land is a Desert OTHER than Arid Archway itself.
 */
class AridArchwayScenarioTest : FunSpec({

    fun GameTestDriver.inHand(playerId: EntityId, name: String): Boolean =
        getHand(playerId).any { state.getEntity(it)?.get<CardComponent>()?.name == name }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AridArchway))
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("returning another Desert surveils 1; the land enters tapped") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val otherDesert = driver.putLandOnBattlefield(me, "Desert")
        driver.putCardOnTopOfLibrary(me, "Island")

        val archway = driver.putCardInHand(me, "Arid Archway")
        driver.playLand(me, archway)

        // Enters tapped.
        driver.isTapped(archway) shouldBe true

        // ETB trigger asks which land to return.
        (driver.pendingDecision as ChooseTargetsDecision)
        driver.submitTargetSelection(me, listOf(otherDesert))
        driver.bothPass()

        // The Desert was returned to hand.
        driver.inHand(me, "Desert") shouldBe true
        // Another Desert returned -> surveil 1 pauses for the keep/graveyard choice.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("returning a non-Desert land does not surveil") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val island = driver.putLandOnBattlefield(me, "Island")

        val archway = driver.putCardInHand(me, "Arid Archway")
        driver.playLand(me, archway)

        (driver.pendingDecision as ChooseTargetsDecision)
        driver.submitTargetSelection(me, listOf(island))
        driver.bothPass()

        driver.inHand(me, "Island") shouldBe true
        // Non-Desert returned -> no surveil; resolution completes.
        driver.isPaused shouldBe false
    }

    test("returning Arid Archway itself does not surveil (not 'another' Desert)") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val archway = driver.putCardInHand(me, "Arid Archway")
        driver.playLand(me, archway)

        // The only land you control is the Archway itself; return it.
        (driver.pendingDecision as ChooseTargetsDecision)
        driver.submitTargetSelection(me, listOf(archway))
        driver.bothPass()

        driver.inHand(me, "Arid Archway") shouldBe true
        // Returned land is the source itself -> not "another" Desert -> no surveil.
        driver.isPaused shouldBe false
    }
})
