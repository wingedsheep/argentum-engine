package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureLastKnown
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Entered this turn" about a permanent that has since left the battlefield reads it as it last
 * existed there (CR 608.2h): the battlefield-exit snapshot freezes the turn it entered, and the
 * answer is true only while that is still the current turn — a card left in a graveyard doesn't
 * keep "entered this turn" into later turns.
 */
class EnteredThisTurnLastKnownTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val enteredThisTurn = GameObjectFilter.Any.enteredThisTurn()

    fun GameTestDriver.matches(entity: EntityId) =
        services.predicateEvaluator.matches(
            state, state.projectedState, entity, enteredThisTurn,
            PredicateContext(controllerId = activePlayer!!)
        )

    fun GameTestDriver.departedWith(snapshot: (EntityId) -> EntitySnapshot): EntityId {
        val gone = putCardInGraveyard(activePlayer!!, "Grizzly Bears")
        replaceState(state.updateEntity(gone) { it.with(LastKnownPermanentComponent(snapshot(gone))) })
        return gone
    }

    test("the live marker answers on the battlefield, and the exit snapshot freezes it") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.activePlayer!!, "Grizzly Bears")
        d.matches(bears) shouldBe false
        d.replaceState(d.state.updateEntity(bears) { it.with(EnteredThisTurnComponent) })
        d.matches(bears) shouldBe true

        captureLastKnown(d.state, bears).enteredOnTurn shouldBe d.state.turnNumber
    }

    test("off the battlefield, a snapshot from this turn matches") {
        val d = driver()
        val gone = d.departedWith { EntitySnapshot(entityId = it, enteredOnTurn = d.state.turnNumber) }
        d.matches(gone) shouldBe true
    }

    test("off the battlefield, a snapshot from an earlier turn or without the marker doesn't match") {
        val d = driver()
        val earlier = d.departedWith { EntitySnapshot(entityId = it, enteredOnTurn = d.state.turnNumber - 1) }
        val never = d.departedWith { EntitySnapshot(entityId = it) }
        d.matches(earlier) shouldBe false
        d.matches(never) shouldBe false
    }

    test("matchesSnapshot answers from the frozen turn too") {
        val d = driver()
        val ctx = PredicateContext(controllerId = d.activePlayer!!)
        val id = d.putCardInGraveyard(d.activePlayer!!, "Grizzly Bears")
        val evaluator = d.services.predicateEvaluator
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id, enteredOnTurn = d.state.turnNumber), enteredThisTurn, ctx) shouldBe true
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id, enteredOnTurn = d.state.turnNumber - 1), enteredThisTurn, ctx) shouldBe false
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id), enteredThisTurn, ctx) shouldBe false
    }
})
