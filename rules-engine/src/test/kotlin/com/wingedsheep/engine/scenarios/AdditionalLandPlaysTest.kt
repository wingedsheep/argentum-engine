package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for effects that grant additional land plays per turn.
 *
 * Summer Bloom: {1}{G} Sorcery - You may play up to three additional lands this turn.
 */
class AdditionalLandPlaysTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 30,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Summer Bloom allows playing three additional lands") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to cast Summer Bloom (costs {1}{G})
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)

        // Put Summer Bloom in hand
        val summerBloom = driver.putCardInHand(player, "Summer Bloom")

        // Cast Summer Bloom
        val castResult = driver.castSpell(player, summerBloom)
        castResult.isSuccess shouldBe true

        // Pass priority to resolve
        driver.bothPass()

        // Verify player now has 4 land drops (1 normal + 3 from Summer Bloom)
        val landDrops = driver.state.getEntity(player)?.get<LandDropsComponent>()
        landDrops shouldNotBe null
        landDrops!!.remaining shouldBe 4

        // Play first land (normal)
        val forest1 = driver.findCardInHand(player, "Forest")!!
        driver.playLand(player, forest1).isSuccess shouldBe true

        // Should have 3 remaining
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe 3

        // Play second land (first additional)
        val forest2 = driver.findCardInHand(player, "Forest")!!
        driver.playLand(player, forest2).isSuccess shouldBe true

        // Should have 2 remaining
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe 2

        // Play third land (second additional)
        val forest3 = driver.findCardInHand(player, "Forest")!!
        driver.playLand(player, forest3).isSuccess shouldBe true

        // Should have 1 remaining
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe 1

        // Play fourth land (third additional)
        val forest4 = driver.findCardInHand(player, "Forest")!!
        driver.playLand(player, forest4).isSuccess shouldBe true

        // Should have 0 remaining
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe 0

        // Verify we can't play a fifth land
        val forest5 = driver.findCardInHand(player, "Forest")
        if (forest5 != null) {
            val fifthResult = driver.submitExpectFailure(PlayLand(player, forest5))
            fifthResult.isSuccess shouldBe false
        }

        // Verify 4 lands on battlefield
        driver.getLands(player).size shouldBe 4
    }

    test("Summer Bloom effect does not persist to next turn") {
        val driver = createDriver()
        val startingPlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to cast Summer Bloom
        driver.giveMana(startingPlayer, Color.GREEN, 1)
        driver.giveColorlessMana(startingPlayer, 1)

        // Put Summer Bloom in hand and cast it
        val summerBloom = driver.putCardInHand(startingPlayer, "Summer Bloom")
        driver.castSpell(startingPlayer, summerBloom)
        driver.bothPass()

        // Verify land drops increased
        var landDrops = driver.state.getEntity(startingPlayer)?.get<LandDropsComponent>()
        landDrops shouldNotBe null
        landDrops!!.remaining shouldBe 4

        // Play one land (using 1 of 4 available)
        val forest1 = driver.findCardInHand(startingPlayer, "Forest")!!
        driver.playLand(startingPlayer, forest1)

        // Verify remaining is 3
        landDrops = driver.state.getEntity(startingPlayer)?.get<LandDropsComponent>()
        landDrops!!.remaining shouldBe 3

        // End the turn and pass through opponent's turn back to us
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Pass through the game until we're back to the starting player's main phase
        // This handles any number of turn transitions
        var passes = 0
        while ((driver.activePlayer != startingPlayer || driver.currentStep != Step.PRECOMBAT_MAIN) && passes < 50) {
            if (driver.state.priorityPlayerId != null) {
                driver.passPriority(driver.state.priorityPlayerId!!)
            }
            passes++
        }

        // Verify we're back at starting player's main phase
        driver.activePlayer shouldBe startingPlayer
        driver.currentStep shouldBe Step.PRECOMBAT_MAIN

        // Land drops should be reset to the normal 1 (maxPerTurn is still 1)
        landDrops = driver.state.getEntity(startingPlayer)?.get<LandDropsComponent>()
        landDrops shouldNotBe null
        landDrops!!.remaining shouldBe 1
        landDrops.maxPerTurn shouldBe 1
    }

    test("cannot play land without available land drops") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Play the normal land drop
        val forest1 = driver.findCardInHand(player, "Forest")!!
        driver.playLand(player, forest1).isSuccess shouldBe true

        // Verify can't play another land
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe 0

        val forest2 = driver.findCardInHand(player, "Forest")
        if (forest2 != null) {
            val result = driver.submitExpectFailure(PlayLand(player, forest2))
            result.isSuccess shouldBe false
        }
    }
})
