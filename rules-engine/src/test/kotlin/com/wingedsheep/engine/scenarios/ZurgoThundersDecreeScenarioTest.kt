package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.ZurgoThundersDecree
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual

/**
 * Tests for Zurgo, Thunder's Decree (TDM, {R}{W}{B}, 2/4).
 *
 * Mobilize 2 makes two Warrior tokens on attack that would normally be sacrificed at the next end
 * step. Zurgo's static — "During your end step, Warrior tokens you control have 'This token can't
 * be sacrificed.'" — makes the mobilize sacrifice no-op, so the tokens survive.
 */
class ZurgoThundersDecreeScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZurgoThundersDecree))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    fun GameTestDriver.warriorTokens(controller: com.wingedsheep.sdk.model.EntityId): List<com.wingedsheep.sdk.model.EntityId> =
        state.getBattlefield().filter { id ->
            val card = state.getEntity(id)?.get<CardComponent>() ?: return@filter false
            card.name.contains("Warrior", ignoreCase = true) &&
                state.projectedState.getController(id) == controller
        }

    test("Mobilize Warrior tokens survive the end step thanks to Zurgo") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val zurgo = driver.putCreatureOnBattlefield(driver.player1, "Zurgo, Thunder's Decree")
        driver.removeSummoningSickness(zurgo)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(driver.player1, mapOf(zurgo to driver.player2))
        // Resolve the Mobilize trigger → two tapped, attacking Warrior tokens.
        driver.bothPass()

        driver.warriorTokens(driver.player1).size shouldBeGreaterThanOrEqual 2

        // Advance to player 1's end step; the mobilize delayed sacrifice fires here, but Zurgo's
        // static makes the tokens "can't be sacrificed" during the end step.
        driver.passPriorityUntil(Step.END)
        var safety = 0
        while (driver.activePlayer != driver.player1 && safety < 50) {
            driver.bothPass()
            driver.passPriorityUntil(Step.END)
            safety++
        }

        // During player 1's end step, the projected flag is present on the Warrior tokens.
        val projected = projector.project(driver.state)
        val tokens = driver.warriorTokens(driver.player1)
        tokens.size shouldBeGreaterThanOrEqual 2
        tokens.all { projected.hasKeyword(it, AbilityFlag.CANT_BE_SACRIFICED) } shouldBe true

        // Drain the end-step delayed sacrifice; the tokens are not sacrificed.
        driver.bothPass()
        driver.warriorTokens(driver.player1).size shouldBeGreaterThanOrEqual 2
    }
})
