package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.RiddlesInTheDark
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Scenario tests for Riddles in the Dark's concealed-pile spell pipeline. */
class RiddlesInTheDarkScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(RiddlesInTheDark)
    }

    fun GameTestDriver.revealedTo(card: EntityId, player: EntityId): Boolean =
        state.getEntity(card)?.get<RevealedToComponent>()?.isRevealedTo(player) == true

    fun GameTestDriver.castRiddles(active: EntityId) {
        val spell = putCardInHand(active, "Riddles in the Dark")
        giveMana(active, Color.BLUE, 1)
        giveColorlessMana(active, 2)
        castSpell(active, spell).isSuccess shouldBe true
        bothPass()
    }

    test("controller splits four cards, opponent sees only face-up cards and chooses their destinations") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        driver.castRiddles(active)

        val handBeforeChoice = driver.getHandSize(active)
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe active
        split.options.size shouldBe 4
        listOf(c1, c2, c3, c4).forEach { driver.revealedTo(it, active) shouldBe true }
        listOf(c1, c2, c3, c4).forEach { driver.revealedTo(it, opponent) shouldBe false }

        driver.submitDecision(active, CardsSelectedResponse(split.id, listOf(c4, c3)))
        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe opponent
        listOf(c4, c3).forEach { driver.revealedTo(it, opponent) shouldBe true }
        listOf(c2, c1).forEach { driver.revealedTo(it, opponent) shouldBe false }
        choose.optionCardIds?.get(0) shouldBe listOf(c4, c3)
        choose.optionCardIds?.get(1) shouldBe listOf(c2, c1)

        driver.submitDecision(opponent, OptionChosenResponse(choose.id, 1))
        val hand = driver.state.getZone(ZoneKey(active, Zone.HAND))
        listOf(c2, c1).forEach { hand.contains(it) shouldBe true }
        driver.getHandSize(active) shouldBe handBeforeChoice + 2
        val graveyard = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD))
        listOf(c4, c3).forEach { graveyard.contains(it) shouldBe true }
    }

    test("an empty face-up pile is legal and the opponent may choose it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cards = listOf(
            driver.putCardOnTopOfLibrary(active, "Island"),
            driver.putCardOnTopOfLibrary(active, "Forest"),
            driver.putCardOnTopOfLibrary(active, "Mountain"),
            driver.putCardOnTopOfLibrary(active, "Plains")
        )
        driver.castRiddles(active)
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(active, CardsSelectedResponse(split.id, emptyList()))

        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        cards.forEach { driver.revealedTo(it, opponent) shouldBe false }
        val emptyOption = choose.optionCardIds?.entries?.single { it.value.isEmpty() }?.key!!
        driver.submitDecision(opponent, OptionChosenResponse(choose.id, emptyOption))

        val graveyard = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD))
        cards.forEach { graveyard.contains(it) shouldBe true }
    }

    test("an empty library resolves without asking either player to make a pile") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 1), startingLife = 20)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val library = ZoneKey(active, Zone.LIBRARY)
        var emptied = driver.state
        driver.state.getZone(library).forEach { card ->
            emptied = emptied.removeFromZone(library, card).withoutEntity(card)
        }
        driver.replaceState(emptied)

        driver.castRiddles(active)

        driver.isPaused shouldBe false
        driver.state.getZone(library).size shouldBe 0
        driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)).size shouldBe 1
    }
})
