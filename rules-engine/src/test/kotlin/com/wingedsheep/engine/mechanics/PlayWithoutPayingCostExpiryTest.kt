package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CleanupPhaseManager
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * End-of-turn cleanup of [PlayWithoutPayingCostComponent]: a waiver with no floor dies at this
 * cleanup, a turn-keyed waiver ("until the end of your next turn", Ignite the Future) survives
 * every cleanup until its controller's turn at or after `expiresAfterTurn`, and a permanent one
 * is never touched.
 */
class PlayWithoutPayingCostExpiryTest : FunSpec({

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val card = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        return Triple(driver, active, card)
    }

    fun survivesCleanup(
        driver: GameTestDriver,
        card: com.wingedsheep.sdk.model.EntityId,
        component: PlayWithoutPayingCostComponent,
        turnNumber: Int,
        activePlayer: com.wingedsheep.sdk.model.EntityId,
    ): Boolean {
        val state = driver.state.updateEntity(card) { it.with(component) }
            .copy(turnNumber = turnNumber, activePlayerId = activePlayer)
        val cleanup = CleanupPhaseManager(
            driver.cardRegistry, DecisionHandler(), conditionEvaluator = driver.services.conditionEvaluator
        )
        return cleanup.cleanupEndOfTurn(state).getEntity(card)?.get<PlayWithoutPayingCostComponent>() != null
    }

    test("a waiver with no floor is cleared at this turn's cleanup") {
        val (driver, me, card) = setup()
        survivesCleanup(driver, card, PlayWithoutPayingCostComponent(me), 3, me) shouldBe false
    }

    test("a turn-keyed waiver survives until its controller's turn at or after the floor") {
        val (driver, me, card) = setup()
        val opponent = driver.getOpponent(me)
        val waiver = PlayWithoutPayingCostComponent(me, expiresAfterTurn = 5)
        survivesCleanup(driver, card, waiver, 3, me) shouldBe true
        survivesCleanup(driver, card, waiver, 4, opponent) shouldBe true
        survivesCleanup(driver, card, waiver, 5, opponent) shouldBe true
        survivesCleanup(driver, card, waiver, 5, me) shouldBe false
    }

    test("expiryControllerId, not the grantee, decides whose turn closes the window") {
        val (driver, me, card) = setup()
        val opponent = driver.getOpponent(me)
        val waiver = PlayWithoutPayingCostComponent(me, expiresAfterTurn = 5, expiryControllerId = opponent)
        survivesCleanup(driver, card, waiver, 5, me) shouldBe true
        survivesCleanup(driver, card, waiver, 5, opponent) shouldBe false
    }

    test("a permanent waiver is never cleared by cleanup") {
        val (driver, me, card) = setup()
        survivesCleanup(driver, card, PlayWithoutPayingCostComponent(me, permanent = true), 9, me) shouldBe true
    }
})
