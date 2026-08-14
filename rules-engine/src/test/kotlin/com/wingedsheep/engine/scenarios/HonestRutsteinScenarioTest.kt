package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HonestRutstein
import com.wingedsheep.mtg.sets.definitions.otj.cards.RictusRobber
import com.wingedsheep.mtg.sets.definitions.otj.cards.StingerbackTerror
import com.wingedsheep.mtg.sets.definitions.otj.cards.TumbleweedRising
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Honest Rutstein. */
class HonestRutsteinScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + TumbleweedRising + StingerbackTerror + RictusRobber + HonestRutstein
        )
        return driver
    }

    fun GameTestDriver.handCount(playerId: EntityId): Int =
        state.getZone(playerId, Zone.HAND).size

    fun GameTestDriver.tokenSubtypes(id: EntityId): Set<String> =
        state.getEntity(id)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()

    // ---------------------------------------------------------------------
    // Honest Rutstein
    // ---------------------------------------------------------------------

    test("Honest Rutstein returns a target creature card from your graveyard to your hand on ETB") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val deadBear = driver.putCardInGraveyard(player, "Grizzly Bears")

        val rutstein = driver.putCardInHand(player, "Honest Rutstein")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        val handAfterCast = driver.handCount(player) - 1 // Rutstein leaves hand when cast

        driver.castSpell(player, rutstein)
        driver.bothPass() // resolve creature -> ETB trigger goes on stack
        // ETB targets the only creature card in the graveyard.
        if (driver.state.pendingDecision != null) {
            driver.submitTargetSelection(player, listOf(deadBear))
        }
        driver.bothPass() // resolve the ETB trigger

        // The bear should be back in hand.
        driver.state.getZone(player, Zone.HAND).contains(deadBear) shouldBe true
        driver.handCount(player) shouldBe (handAfterCast + 1)
    }
})
