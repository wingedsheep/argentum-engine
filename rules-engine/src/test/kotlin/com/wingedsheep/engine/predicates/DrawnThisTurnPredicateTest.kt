package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.DrawnThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.DrawnThisTurn` — a card in a hand that got there by being drawn this turn
 * (Sylvan Library). Opening hands aren't draws during a turn, a drawn card that leaves the hand and
 * comes back is a new object (CR 400.7), and the stamp is wiped when the next turn begins.
 */
class DrawnThisTurnPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val filter = GameObjectFilter.Any.drawnThisTurn()

    fun GameTestDriver.drawnInHand(player: EntityId): List<EntityId> =
        getHand(player).filter {
            evaluator.matches(state, state.projectedState, it, filter, PredicateContext(controllerId = player))
        }

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val first = driver.activePlayer!!
        val second = driver.getOpponent(first)
        // The starting player skips their first draw; the second player draws in their draw step.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = second)
        return driver to second
    }

    test("only the card drawn in the draw step matches, not the opening hand") {
        val (driver, player) = setUp()
        driver.getHand(player) shouldHaveSize 8
        driver.drawnInHand(player) shouldHaveSize 1
    }

    test("a drawn card that leaves the hand and returns is a new object and no longer matches") {
        val (driver, player) = setUp()
        val drawn = driver.drawnInHand(player).single()
        val hand = ZoneKey(player, Zone.HAND)
        val graveyard = ZoneKey(player, Zone.GRAVEYARD)
        driver.replaceState(driver.state.moveToZone(drawn, hand, graveyard).moveToZone(drawn, graveyard, hand))

        (drawn in driver.getHand(player)) shouldBe true
        driver.drawnInHand(player) shouldHaveSize 0
    }

    test("the stamp is wiped when the next turn begins") {
        val (driver, player) = setUp()
        val drawn = driver.drawnInHand(player).single()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = driver.getOpponent(player))

        (drawn in driver.getHand(player)) shouldBe true
        driver.state.getEntity(drawn)?.has<DrawnThisTurnComponent>() shouldBe false
        driver.drawnInHand(player) shouldHaveSize 0
    }
})
