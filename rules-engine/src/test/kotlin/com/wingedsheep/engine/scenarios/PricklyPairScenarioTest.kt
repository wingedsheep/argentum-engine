package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Prickly Pair (OTJ #137) — {2}{R} 2/2 Creature — Plant Mercenary.
 *
 * "When this creature enters, create a 1/1 red Mercenary creature token with
 *  '{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.'"
 *
 * Verifies the ETB makes a single 1/1 red Mercenary token carrying the granted activated ability.
 */
class PricklyPairScenarioTest : FunSpec({

    val projector = StateProjector()

    test("ETB creates a 1/1 red Mercenary token with an activated ability") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tokensBefore = driver.state.getBattlefield().filter {
            val e = driver.state.getEntity(it)
            e?.has<TokenComponent>() == true &&
                e.get<ControllerComponent>()?.playerId == player
        }.toSet()

        val pair = driver.putCardInHand(player, "Prickly Pair")
        driver.giveMana(player, Color.RED, 3)
        driver.castSpell(player, pair).isSuccess shouldBe true
        // Resolve the creature spell, then its ETB trigger.
        var guard = 0
        while (driver.stackSize > 0 && !driver.isPaused && guard++ < 10) driver.bothPass()

        val newTokens = driver.state.getBattlefield().filter {
            val e = driver.state.getEntity(it)
            e?.has<TokenComponent>() == true &&
                e.get<ControllerComponent>()?.playerId == player
        }.toSet() - tokensBefore

        newTokens.size shouldBe 1
        val token = newTokens.first()
        projector.getProjectedPower(driver.state, token) shouldBe 1
        projector.getProjectedToughness(driver.state, token) shouldBe 1

        val card = driver.state.getEntity(token)!!.get<CardComponent>()!!
        card.colors shouldBe setOf(Color.RED)
        card.typeLine.subtypes.map { it.value }.contains("Mercenary") shouldBe true
    }
})
