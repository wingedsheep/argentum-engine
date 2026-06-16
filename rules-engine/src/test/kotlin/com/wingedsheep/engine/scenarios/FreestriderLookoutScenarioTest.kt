package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.FreestriderLookout
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Freestrider Lookout — {2}{G} 3/3 Creature — Human Rogue, Reach.
 *
 * "Whenever you commit a crime, look at the top five cards of your library. You may put a land
 *  card from among them onto the battlefield tapped. Put the rest on the bottom of your library
 *  in a random order. This ability triggers only once each turn."
 */
class FreestriderLookoutScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FreestriderLookout)
        return driver
    }

    fun GameTestDriver.battlefieldNames(playerId: EntityId): List<String> =
        state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).mapNotNull {
            state.getEntity(it)?.get<CardComponent>()?.name
        }

    test("committing a crime lets you put a land from the top five onto the battlefield tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 60), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Freestrider Lookout")

        // Seed a known top five: a single Forest (land) buried among non-land cards
        // (Lightning Bolt instants), so the look-at-five sees exactly one eligible land.
        // putCardOnTopOfLibrary pushes onto the top, so the LAST push ends up on top.
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")

        val landsBefore = driver.battlefieldNames(me).count { it == "Forest" }
        landsBefore shouldBe 0

        // Commit a crime: cast Lightning Bolt targeting the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime event -> look-at-five trigger on stack
        driver.bothPass() // resolve trigger -> pauses for the land selection

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val select = driver.pendingDecision as SelectCardsDecision
        // Only the Forest among the five is an eligible land choice.
        select.options.contains(forest) shouldBe true

        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(forest)))

        // "Put the rest on the bottom in a random order" needs no player input — it resolves
        // automatically. The pipeline should be done.
        driver.isPaused shouldBe false

        // The Forest is now on the battlefield, tapped.
        driver.battlefieldNames(me).count { it == "Forest" } shouldBe 1
        driver.isTapped(forest) shouldBe true
    }

    test("the trigger fires only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 60), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Freestrider Lookout")

        // First crime — declines the land (selects none), so no land enters.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe true
        val select = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = emptyList()))
        driver.isPaused shouldBe false

        // Second crime same turn — ability triggers only once each turn, so no new decision.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe false
    }
})
