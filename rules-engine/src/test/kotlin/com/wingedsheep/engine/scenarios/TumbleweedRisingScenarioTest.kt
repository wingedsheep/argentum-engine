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

/** Scenario tests for Tumbleweed Rising. */
class TumbleweedRisingScenarioTest : FunSpec({

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
    // Tumbleweed Rising
    // ---------------------------------------------------------------------

    test("Tumbleweed Rising creates an X/X Elemental where X is the greatest power among creatures you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Greatest power among my creatures: Grizzly Bears (2) and Savannah Lions (1) -> 2.
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Savannah Lions")

        val spell = driver.putCardInHand(player, "Tumbleweed Rising")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, spell)
        driver.bothPass()

        val token = driver.getCreatures(player).single { driver.tokenSubtypes(it).contains("Elemental") }
        projector.getProjectedPower(driver.state, token) shouldBe 2
        projector.getProjectedToughness(driver.state, token) shouldBe 2
    }
})
