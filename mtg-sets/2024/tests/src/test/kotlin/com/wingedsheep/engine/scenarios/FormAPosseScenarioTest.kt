package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Form a Posse (OTJ #204) — {X}{R}{W} Sorcery.
 *
 *   "Create X 1/1 red Mercenary creature tokens with "{T}: Target creature you control gets
 *    +1/+0 until end of turn. Activate only as a sorcery.""
 *
 * Verifies the X-many token count (DynamicAmount.XValue) and that each token is a 1/1 red
 * Mercenary carrying the granted tap ability.
 */
class FormAPosseScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("X=3 creates three 1/1 red Mercenary tokens, each with the granted tap ability") {
        val driver = createDriver()
        val player = driver.player1
        val projector = StateProjector()

        // {X}{R}{W} with X=3 → 5 mana.
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveColorlessMana(player, 3)

        val posse = driver.putCardInHand(player, "Form a Posse")
        val cast = driver.castXSpell(player, posse, xValue = 3)
        cast.error shouldBe null
        driver.bothPass()

        val mercenaries = driver.getCreatures(player).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Mercenary Token"
        }
        mercenaries.size shouldBe 3

        val projected = projector.project(driver.state)
        for (token in mercenaries) {
            val card = driver.state.getEntity(token)!!.get<CardComponent>()!!
            driver.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
            card.colors shouldBe setOf(Color.RED)
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            // The token was created with its {T} ability.
            card.copyActivatedAbilities.isNotEmpty() shouldBe true
        }
    }

    test("X=0 creates no tokens") {
        val driver = createDriver()
        val player = driver.player1

        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.WHITE, 1)

        val posse = driver.putCardInHand(player, "Form a Posse")
        val before = driver.getCreatures(player).size
        val cast = driver.castXSpell(player, posse, xValue = 0)
        cast.error shouldBe null
        driver.bothPass()

        driver.getCreatures(player).size shouldBe before
    }
})
