package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
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
 * "Tapped" / "untapped" about a permanent that has since left the battlefield reads it as it last
 * existed there (CR 608.2h): the battlefield-exit snapshot freezes its tap state, so an
 * intervening "if [this] is untapped" answered after the permanent is gone (Voodoo Doll) sees how
 * it left.
 */
class TapStateLastKnownTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val tapped = GameObjectFilter.Any.tapped()
    val untapped = GameObjectFilter.Any.untapped()

    fun GameTestDriver.matches(entity: EntityId, filter: GameObjectFilter) =
        services.predicateEvaluator.matches(
            state, state.projectedState, entity, filter,
            PredicateContext(controllerId = activePlayer!!)
        )

    fun GameTestDriver.departedWith(snapshot: (EntityId) -> EntitySnapshot): EntityId {
        val gone = putCardInGraveyard(activePlayer!!, "Grizzly Bears")
        replaceState(state.updateEntity(gone) { it.with(LastKnownPermanentComponent(snapshot(gone))) })
        return gone
    }

    test("the exit snapshot freezes the live tap state") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.activePlayer!!, "Grizzly Bears")
        captureLastKnown(d.state, bears).wasTapped shouldBe false
        d.replaceState(d.state.updateEntity(bears) { it.with(TappedComponent) })
        captureLastKnown(d.state, bears).wasTapped shouldBe true
        d.matches(bears, tapped) shouldBe true
        d.matches(bears, untapped) shouldBe false
    }

    test("off the battlefield, a permanent that left tapped is tapped, not untapped") {
        val d = driver()
        val gone = d.departedWith { EntitySnapshot(entityId = it, wasTapped = true) }
        d.matches(gone, tapped) shouldBe true
        d.matches(gone, untapped) shouldBe false
    }

    test("off the battlefield, a permanent that left untapped is untapped, not tapped") {
        val d = driver()
        val gone = d.departedWith { EntitySnapshot(entityId = it, wasTapped = false) }
        d.matches(gone, tapped) shouldBe false
        d.matches(gone, untapped) shouldBe true
    }

    test("matchesSnapshot answers from the frozen tap state") {
        val d = driver()
        val ctx = PredicateContext(controllerId = d.activePlayer!!)
        val id = d.putCardInGraveyard(d.activePlayer!!, "Grizzly Bears")
        val evaluator = d.services.predicateEvaluator
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id, wasTapped = true), tapped, ctx) shouldBe true
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id, wasTapped = true), untapped, ctx) shouldBe false
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id, wasTapped = false), untapped, ctx) shouldBe true
        evaluator.matchesSnapshot(d.state, EntitySnapshot(id, wasTapped = false), tapped, ctx) shouldBe false
    }
})
