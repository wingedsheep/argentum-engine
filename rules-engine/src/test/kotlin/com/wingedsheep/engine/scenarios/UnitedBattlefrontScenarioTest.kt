package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * United Battlefront (TDM #32) — {3}{W} Sorcery.
 *
 * "Look at the top seven cards of your library. Put up to two noncreature, nonland permanent cards
 *  with mana value 3 or less from among them onto the battlefield. Put the rest on the bottom of
 *  your library in a random order."
 *
 * The "look at" clause is information the player is owed unconditionally, so the selection prompt
 * shows all seven cards — the ineligible ones greyed out — even when none of them can be kept.
 */
class UnitedBattlefrontScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.library(playerId: EntityId): List<EntityId> =
        state.getZone(ZoneKey(playerId, Zone.LIBRARY))

    fun GameTestDriver.battlefieldNames(playerId: EntityId): List<String> =
        state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).mapNotNull {
            state.getEntity(it)?.get<CardComponent>()?.name
        }

    /** Cast the sorcery and resolve it up to the selection pause. */
    fun castAndResolve(driver: GameTestDriver, playerId: EntityId) {
        val battlefront = driver.putCardInHand(playerId, "United Battlefront")
        driver.giveMana(playerId, Color.WHITE, 4)
        driver.castSpell(playerId, battlefront)
        driver.bothPass()
    }

    test("shows all seven cards even when none of them can be put onto the battlefield") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A library of Mountains: every one of the top seven is a land, so nothing is eligible.
        val topSeven = driver.library(p1).take(7)

        castAndResolve(driver, p1)

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val select = driver.pendingDecision as SelectCardsDecision

        // Nothing is selectable, but the player still sees the seven cards they looked at.
        select.options.shouldBeEmpty()
        select.nonSelectableOptions shouldContainExactlyInAnyOrder topSeven
        select.minSelections shouldBe 0
        select.maxSelections shouldBe 0
        topSeven.forEach { cardId -> select.cardInfo?.get(cardId)?.name shouldBe "Mountain" }

        driver.submitDecision(p1, CardsSelectedResponse(decisionId = select.id, selectedCards = emptyList()))
        driver.isPaused shouldBe false

        // All seven went to the bottom; none of them stuck around on the battlefield.
        driver.library(p1).takeLast(7) shouldContainExactlyInAnyOrder topSeven
        driver.battlefieldNames(p1) shouldContainExactlyInAnyOrder emptyList()
    }

    test("puts up to two matching cards onto the battlefield and the rest on the bottom") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // putCardOnTopOfLibrary prepends, so the last push ends up on top:
        // top seven = [Copper Tablet, Sol Ring, Mountain x5].
        val solRing = driver.putCardOnTopOfLibrary(p1, "Sol Ring")
        val copperTablet = driver.putCardOnTopOfLibrary(p1, "Copper Tablet")
        val mountains = driver.library(p1).drop(2).take(5)

        castAndResolve(driver, p1)

        driver.isPaused shouldBe true
        val select = driver.pendingDecision as SelectCardsDecision
        select.options shouldContainExactlyInAnyOrder listOf(solRing, copperTablet)
        select.nonSelectableOptions shouldContainExactlyInAnyOrder mountains
        select.maxSelections shouldBe 2

        driver.submitDecision(
            p1,
            CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(solRing, copperTablet))
        )
        driver.isPaused shouldBe false

        driver.battlefieldNames(p1) shouldContainExactlyInAnyOrder listOf("Sol Ring", "Copper Tablet")
        driver.library(p1).takeLast(5) shouldContainExactlyInAnyOrder mountains
    }

    test("a single matching card can be kept, and the player may keep none") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Icy Manipulator is a noncreature, nonland permanent, but mana value 4 — too expensive.
        val icy = driver.putCardOnTopOfLibrary(p1, "Icy Manipulator")
        val solRing = driver.putCardOnTopOfLibrary(p1, "Sol Ring")

        castAndResolve(driver, p1)

        val select = driver.pendingDecision as SelectCardsDecision
        select.options shouldContainExactlyInAnyOrder listOf(solRing)

        // Declining the choice is legal ("up to two") and sends everything to the bottom.
        driver.submitDecision(p1, CardsSelectedResponse(decisionId = select.id, selectedCards = emptyList()))
        driver.isPaused shouldBe false

        driver.battlefieldNames(p1) shouldContainExactlyInAnyOrder emptyList()
        val bottomSeven = driver.library(p1).takeLast(7)
        bottomSeven shouldContain solRing
        bottomSeven shouldContain icy
    }
})
