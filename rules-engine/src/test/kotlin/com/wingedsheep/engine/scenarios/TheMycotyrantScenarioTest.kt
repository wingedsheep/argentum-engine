package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.BroodrageMycoid
import com.wingedsheep.mtg.sets.definitions.lci.cards.TheMycotyrant
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Mycotyrant (LCI #235): {1}{B}{G} Legendary Creature — Elder Fungus, mythic.
 *
 * - Trample
 * - Its power and toughness are each equal to the number of creatures you control that
 *   are Fungi and/or Saprolings (characteristic-defining ability — counts itself, since
 *   it is a Fungus).
 * - At the beginning of your end step, create X 1/1 black Fungus tokens with "This token
 *   can't block," where X = the number of times you descended this turn.
 *
 * Tests:
 * 1. Trample is present.
 * 2. The CDA sets P/T to the count of Fungi/Saprolings you control (itself + another
 *    Fungus), and excludes creatures of other types.
 * 3. The end-step trigger creates exactly X Fungus tokens = the descend count (2 here).
 * 4. No tokens are created when you have not descended this turn (X = 0).
 */
class TheMycotyrantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheMycotyrant, BroodrageMycoid))
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    /**
     * Move a card to the graveyard via [ZoneTransitionService] to fire the zone-change
     * event and increment the descend counter (CR 700.11).
     */
    fun GameTestDriver.descend(entityId: EntityId) {
        val result = ZoneTransitionService.moveToZone(
            state = state,
            entityId = entityId,
            destinationZone = Zone.GRAVEYARD
        )
        replaceState(result.state)
    }

    fun GameTestDriver.fungusTokens(playerId: EntityId): List<EntityId> =
        getCreatures(playerId).filter { getCardName(it) == "Fungus Token" }

    test("The Mycotyrant has trample") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val mycotyrant = driver.putCreatureOnBattlefield(player, "The Mycotyrant")

        driver.state.projectedState.hasKeyword(mycotyrant, Keyword.TRAMPLE) shouldBe true
    }

    test("power and toughness equal the number of Fungi/Saprolings you control") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Alone: only The Mycotyrant itself (an Elder Fungus) counts → 1/1.
        val mycotyrant = driver.putCreatureOnBattlefield(player, "The Mycotyrant")
        driver.state.projectedState.getPower(mycotyrant) shouldBe 1
        driver.state.projectedState.getToughness(mycotyrant) shouldBe 1

        // Add another Fungus (Broodrage Mycoid) → 2 Fungi → 2/2.
        driver.putCreatureOnBattlefield(player, "Broodrage Mycoid")
        driver.state.projectedState.getPower(mycotyrant) shouldBe 2
        driver.state.projectedState.getToughness(mycotyrant) shouldBe 2

        // Add a non-Fungus, non-Saproling creature → count unchanged, still 2/2.
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.state.projectedState.getPower(mycotyrant) shouldBe 2
        driver.state.projectedState.getToughness(mycotyrant) shouldBe 2
    }

    test("end step creates X Fungus tokens equal to the number of times you descended") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "The Mycotyrant")

        // Descend twice: two permanent (creature) cards put into the graveyard.
        val bears1 = driver.putCardInHand(player, "Grizzly Bears")
        val bears2 = driver.putCardInHand(player, "Grizzly Bears")
        driver.descend(bears1)
        driver.descend(bears2)

        val tokensBefore = driver.fungusTokens(player).size
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // end-step trigger resolves, creating X = 2 tokens

        val tokensAfter = driver.fungusTokens(player)
        tokensAfter.size shouldBe tokensBefore + 2

        // Each token is a 1/1 that can't block.
        val token = tokensAfter.first()
        driver.state.projectedState.getPower(token) shouldBe 1
        driver.state.projectedState.getToughness(token) shouldBe 1
        driver.state.projectedState.cantBlock(token) shouldBe true
    }

    test("no Fungus tokens are created at end step when you have not descended") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "The Mycotyrant")

        val tokensBefore = driver.fungusTokens(player).size
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.fungusTokens(player).size shouldBe tokensBefore
    }
})
