package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
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

/**
 * Scenario tests for OTJ batch 3 cards: Tumbleweed Rising, Stingerback Terror,
 * Rictus Robber, Honest Rutstein.
 */
class OtjBatch3ScenarioTest : FunSpec({

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

    // ---------------------------------------------------------------------
    // Rictus Robber
    // ---------------------------------------------------------------------

    test("Rictus Robber makes no token when no creature died this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val robber = driver.putCardInHand(player, "Rictus Robber")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 3)
        driver.castSpell(player, robber)
        driver.bothPass() // resolve the creature; intervening-if is false at trigger time -> no trigger

        driver.getCreatures(player).size shouldBe 1
    }

    test("Rictus Robber makes a 2/2 Zombie Rogue token when a creature died this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Record that a creature died this turn (the engine increments this on death).
        driver.replaceState(
            driver.state.updateEntity(player) { container ->
                container.with(CreaturesDiedThisTurnComponent(count = 1))
            }
        )

        val robber = driver.putCardInHand(player, "Rictus Robber")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 3)
        driver.castSpell(player, robber)
        driver.bothPass() // resolve the creature spell -> ETB trigger goes on the stack
        driver.bothPass() // resolve the ETB trigger -> create token

        val creatures = driver.getCreatures(player)
        creatures.size shouldBe 2
        val token = creatures.single { driver.state.getEntity(it)?.get<TokenComponent>() != null }
        driver.tokenSubtypes(token) shouldBe setOf("Zombie", "Rogue")
        projector.getProjectedPower(driver.state, token) shouldBe 2
        projector.getProjectedToughness(driver.state, token) shouldBe 2
    }

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
