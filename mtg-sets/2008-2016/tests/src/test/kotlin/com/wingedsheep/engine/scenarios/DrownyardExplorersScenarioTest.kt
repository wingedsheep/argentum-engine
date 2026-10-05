package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.ShadowsOverInnistradSet
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Drownyard Explorers (SOI) — {3}{U} Creature — Human Wizard 2/4
 *
 * "When this creature enters, investigate."
 */
class DrownyardExplorersScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ShadowsOverInnistradSet.cards)
        driver.registerCard(PredefinedTokens.Clue)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.clues(playerId: EntityId): Int =
        getPermanents(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == "Clue" }

    test("casting it investigates once on entering") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val explorers = driver.putCardInHand(me, "Drownyard Explorers")
        driver.giveMana(me, Color.BLUE, 4)

        driver.castSpell(me, explorers).outcome shouldBe Outcome.Done
        driver.bothPass() // creature spell resolves; ETB trigger goes on the stack
        driver.clues(me) shouldBe 0
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.clues(me) shouldBe 1
        driver.getPermanents(me).count {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Drownyard Explorers"
        } shouldBe 1
    }
})
