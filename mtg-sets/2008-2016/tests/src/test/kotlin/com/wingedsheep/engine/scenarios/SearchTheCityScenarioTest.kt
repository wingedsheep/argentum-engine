package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.SearchTheCity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Search the City {4}{U} — Enchantment (RTR #49).
 *
 * Pins the linked-exile pile and both halves of "play": casting a spell and playing a land with the
 * same name as an exiled card each trigger, a non-matching play doesn't, the returned card is a
 * "may", and emptying the pile sacrifices the enchantment for an extra turn.
 */
class SearchTheCityScenarioTest : FunSpec({

    data class Board(
        val driver: GameTestDriver,
        val you: EntityId,
        val city: EntityId,
        val exiledCourser: EntityId
    )

    /** Cast Search the City with a Centaur Courser on top of a library of Islands. */
    fun setup(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SearchTheCity)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val courser = driver.putCardOnTopOfLibrary(you, "Centaur Courser")
        repeat(5) { driver.putLandOnBattlefield(you, "Island") }
        val city = driver.putCardInHand(you, "Search the City")
        driver.castSpell(you, city).error shouldBe null
        driver.bothPass() // resolve the enchantment
        driver.bothPass() // resolve its enters trigger
        driver.stackSize shouldBe 0
        return Board(driver, you, city, courser)
    }

    fun Board.pile(): List<EntityId> =
        driver.state.getEntity(city)?.get<LinkedExileComponent>()?.exiledIds
            ?.filter { driver.state.getZone(you, Zone.EXILE).contains(it) }
            .orEmpty()

    /** Resolve the top trigger, choosing [pick] (or nothing) from the same-name cards offered. */
    fun Board.resolveChoosing(pick: EntityId?, expectedOptions: List<EntityId>) {
        driver.bothPass()
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.options shouldContainExactlyInAnyOrder expectedOptions
        decision.minSelections shouldBe 0
        driver.submitCardSelection(you, listOfNotNull(pick)).error shouldBe null
        driver.pendingDecision shouldBe null
    }

    test("enters: exiles the top five cards of your library, linked to it") {
        val board = setup()
        val pile = board.pile()
        pile.size shouldBe 5
        pile.contains(board.exiledCourser) shouldBe true
    }

    test("casting a spell with the same name triggers; the matching exiled card goes to hand") {
        val board = setup()
        val (driver, you, city, exiledCourser) = board
        val castCourser = driver.putCardInHand(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 3)
        driver.castSpell(you, castCourser).error shouldBe null
        withClue("the play trigger is above the Courser spell") { driver.stackSize shouldBe 2 }

        board.resolveChoosing(pick = exiledCourser, expectedOptions = listOf(exiledCourser))

        driver.state.getZone(you, Zone.HAND).contains(exiledCourser) shouldBe true
        withClue("four cards still exiled, so the enchantment stays") {
            board.pile().size shouldBe 4
            driver.state.getZone(you, Zone.BATTLEFIELD).contains(city) shouldBe true
            driver.state.getEntity(you)?.get<ExtraTurnsComponent>()?.pending.orEmpty().size shouldBe 0
        }
    }

    test("playing a land with the same name triggers; only cards of that name are offered") {
        val board = setup()
        val (driver, you, _, exiledCourser) = board
        val exiledIslands = board.pile() - exiledCourser
        val island = driver.putCardInHand(you, "Island")
        driver.playLand(you, island).error shouldBe null
        driver.stackSize shouldBe 1

        board.resolveChoosing(pick = exiledIslands.first(), expectedOptions = exiledIslands)

        driver.state.getZone(you, Zone.HAND).contains(exiledIslands.first()) shouldBe true
        board.pile().size shouldBe 4
    }

    test("playing a land with no matching exiled card doesn't trigger") {
        val board = setup()
        val (driver, you, _, _) = board
        val forest = driver.putCardInHand(you, "Forest")
        driver.playLand(you, forest).error shouldBe null
        driver.stackSize shouldBe 0
        board.pile().size shouldBe 5
    }

    test("emptying the pile sacrifices it and you take an extra turn") {
        val board = setup()
        val (driver, you, city, exiledCourser) = board
        (board.pile() - exiledCourser).forEach { driver.moveToGraveyard(it) }
        board.pile() shouldBe listOf(exiledCourser)

        val castCourser = driver.putCardInHand(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 3)
        driver.castSpell(you, castCourser).error shouldBe null
        board.resolveChoosing(pick = exiledCourser, expectedOptions = listOf(exiledCourser))

        driver.state.getZone(you, Zone.GRAVEYARD).contains(city) shouldBe true
        driver.state.getEntity(you)?.get<ExtraTurnsComponent>()?.pending.orEmpty().size shouldBe 1
    }

    test("declining to return the last card keeps the pile, so no sacrifice and no extra turn") {
        val board = setup()
        val (driver, you, city, exiledCourser) = board
        (board.pile() - exiledCourser).forEach { driver.moveToGraveyard(it) }

        val castCourser = driver.putCardInHand(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 3)
        driver.castSpell(you, castCourser).error shouldBe null
        board.resolveChoosing(pick = null, expectedOptions = listOf(exiledCourser))

        board.pile() shouldBe listOf(exiledCourser)
        driver.state.getZone(you, Zone.BATTLEFIELD).contains(city) shouldBe true
        driver.state.getEntity(you)?.get<ExtraTurnsComponent>()?.pending.orEmpty().size shouldBe 0
    }
})
