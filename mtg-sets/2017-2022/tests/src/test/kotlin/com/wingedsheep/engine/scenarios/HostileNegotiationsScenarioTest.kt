package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MayLookAtInExileComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.HostileNegotiations
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hostile Negotiations: exile the top three face down, then the next three face down; you look at
 * both piles and turn one face up; an opponent picks a pile for your hand, the other goes to your
 * graveyard; you lose 3 life.
 *
 * Pins the information asymmetry the composition relies on: the piles are fixed by library
 * position, both start hidden from the opponent, and only the pile you turn face up becomes
 * visible to them before they choose.
 */
class HostileNegotiationsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HostileNegotiations)
        return driver
    }

    fun GameTestDriver.revealedTo(card: EntityId, player: EntityId): Boolean =
        state.getEntity(card)?.get<RevealedToComponent>()?.isRevealedTo(player) == true

    fun GameTestDriver.emptyLibrary(playerId: EntityId) {
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        var newState = state
        for (cardId in state.getZone(libraryZone)) {
            newState = newState.removeFromZone(libraryZone, cardId).withoutEntity(cardId)
        }
        replaceState(newState)
    }

    fun GameTestDriver.castNegotiations(active: EntityId) {
        val spell = putCardInHand(active, "Hostile Negotiations")
        giveMana(active, Color.BLACK, 1)
        giveColorlessMana(active, 3)
        castSpell(active, spell).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("two fixed face-down piles; you flip one; the opponent's pick goes to hand, the other to graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        val c5 = driver.putCardOnTopOfLibrary(active, "Swamp")
        val c6 = driver.putCardOnTopOfLibrary(active, "Grizzly Bears")
        // Library from top: c6, c5, c4, c3, c2, c1 — first pile c6..c4, second pile c3..c1.
        val firstPile = setOf(c6, c5, c4)
        val secondPile = setOf(c3, c2, c1)

        driver.castNegotiations(active)
        val handAfterCast = driver.getHandSize(active)

        // All six are exiled face down: you may look at them, the opponent may not.
        val exile = driver.state.getZone(ZoneKey(active, Zone.EXILE))
        (firstPile + secondPile).forEach {
            exile.contains(it) shouldBe true
            driver.state.getEntity(it)!!.has<FaceDownComponent>() shouldBe true
            driver.state.getEntity(it)!!.get<MayLookAtInExileComponent>()?.mayLook(active) shouldBe true
            driver.revealedTo(it, opponent) shouldBe false
        }

        // You choose which pile to turn face up.
        val flip = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        flip.playerId shouldBe active
        flip.optionCardIds?.get(0)?.toSet() shouldBe firstPile
        flip.optionCardIds?.get(1)?.toSet() shouldBe secondPile
        driver.submitDecision(active, OptionChosenResponse(flip.id, 1))

        // The opponent sees only the pile you turned face up.
        val pick = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe opponent
        pick.optionCardIds?.get(0)?.toSet() shouldBe secondPile
        pick.optionCardIds?.get(1)?.toSet() shouldBe firstPile
        secondPile.forEach { driver.revealedTo(it, opponent) shouldBe true }
        firstPile.forEach { driver.revealedTo(it, opponent) shouldBe false }

        // The opponent gives you the face-down pile.
        driver.submitDecision(opponent, OptionChosenResponse(pick.id, 1))
        driver.isPaused shouldBe false

        val hand = driver.state.getZone(ZoneKey(active, Zone.HAND))
        firstPile.forEach { hand.contains(it) shouldBe true }
        driver.getHandSize(active) shouldBe handAfterCast + 3
        val grave = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD))
        secondPile.forEach { grave.contains(it) shouldBe true }
        driver.getLifeTotal(active) shouldBe 17
    }

    test("an empty library skips the piles but you still lose 3 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.emptyLibrary(active)

        driver.castNegotiations(active)

        driver.isPaused shouldBe false
        driver.getLifeTotal(active) shouldBe 17
    }
})
