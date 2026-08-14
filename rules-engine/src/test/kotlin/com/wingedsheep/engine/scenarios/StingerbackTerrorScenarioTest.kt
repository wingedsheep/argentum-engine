package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HonestRutstein
import com.wingedsheep.mtg.sets.definitions.otj.cards.RictusRobber
import com.wingedsheep.mtg.sets.definitions.otj.cards.StingerbackTerror
import com.wingedsheep.mtg.sets.definitions.otj.cards.TumbleweedRising
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Stingerback Terror. */
class StingerbackTerrorScenarioTest : FunSpec({

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
    // Stingerback Terror
    // ---------------------------------------------------------------------

    test("Stingerback Terror gets -1/-1 for each card in your hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(3) { driver.putCardInHand(player, "Mountain") }
        val handSize = driver.handCount(player)

        val terror = driver.putCreatureOnBattlefield(player, "Stingerback Terror")

        // Base 7/7 minus the current hand size.
        projector.getProjectedPower(driver.state, terror) shouldBe (7 - handSize)
        projector.getProjectedToughness(driver.state, terror) shouldBe (7 - handSize)
    }
})
