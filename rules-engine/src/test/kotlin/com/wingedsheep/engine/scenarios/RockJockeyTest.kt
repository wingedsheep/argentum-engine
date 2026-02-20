package com.wingedsheep.engine.scenarios

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
 * Tests for Rock Jockey.
 *
 * Rock Jockey ({2}{R}, 3/3 Goblin):
 * You can't cast this spell if you've played a land this turn.
 * You can't play lands if you cast this spell this turn.
 */
class RockJockeyTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Rock Jockey can be cast when no land was played this turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana and put Rock Jockey in hand
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        val rockJockey = driver.putCardInHand(player, "Rock Jockey")

        // Cast Rock Jockey - should succeed since no land was played
        val result = driver.castSpell(player, rockJockey)
        result.isSuccess shouldBe true

        // Resolve the spell and ETB trigger
        driver.bothPass()
        driver.bothPass()

        // Rock Jockey should be on the battlefield
        driver.getCreatures(player).any { entityId ->
            driver.state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Rock Jockey"
        } shouldBe true
    }

    test("Rock Jockey cannot be cast after playing a land") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Play a land first
        val mountain = driver.putCardInHand(player, "Mountain")
        driver.playLand(player, mountain).isSuccess shouldBe true

        // Give mana and put Rock Jockey in hand
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        val rockJockey = driver.putCardInHand(player, "Rock Jockey")

        // Try to cast Rock Jockey - should fail
        val result = driver.castSpell(player, rockJockey)
        result.isSuccess shouldBe false
    }

    test("Cannot play a land after casting Rock Jockey") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana and cast Rock Jockey first
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        val rockJockey = driver.putCardInHand(player, "Rock Jockey")

        val castResult = driver.castSpell(player, rockJockey)
        castResult.isSuccess shouldBe true

        // Resolve the spell (Rock Jockey enters battlefield, ETB trigger goes on stack)
        driver.bothPass()

        // Resolve the ETB trigger
        driver.bothPass()

        // Rock Jockey's ETB trigger should have set remaining land drops to 0
        val landDrops = driver.state.getEntity(player)?.get<LandDropsComponent>()
        landDrops shouldNotBe null
        landDrops!!.remaining shouldBe 0

        // Try to play a land - should fail
        val mountain = driver.putCardInHand(player, "Mountain")
        val landResult = driver.playLand(player, mountain)
        landResult.isSuccess shouldBe false
    }
})
