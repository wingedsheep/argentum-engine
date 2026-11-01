package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.battlefield.EnteredFromZoneComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.AnyEnteredOrWasCastFrom
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityEnteredOrWasCastFrom
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Entry provenance for every zone a permanent can come from: the engine stamps
 * [EnteredFromZoneComponent] with the zone a permanent was put onto the battlefield from (a
 * reanimation, a blink, a library search, a land play), and
 * [TriggeringEntityEnteredOrWasCastFrom] / [AnyEnteredOrWasCastFrom] read it alongside the
 * cast-origin markers. The marker belongs to the battlefield object (CR 400.7): it is stripped
 * when the permanent leaves, so a later entry records only its own zone.
 */
class EnteredOrWasCastFromZoneTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val zones = ZoneTransitionService(com.wingedsheep.engine.registry.CardRegistry(), predicateEvaluator = evaluator)

    fun GameTestDriver.moveToBattlefield(id: EntityId): EntityId {
        replaceState(zones.moveToZone(state, id, Zone.BATTLEFIELD).state)
        return id
    }

    fun GameTestDriver.holds(condition: Condition, entity: EntityId): Boolean =
        evaluator.conditions.evaluate(
            state,
            condition,
            EffectContext(
                sourceId = entity,
                controllerId = player1,
                triggeringEntityId = entity,
                pipeline = PipelineState(
                    storedCollections = mapOf(PipelineState.TRIGGER_CAPTURED_COLLECTION to listOf(entity))
                ),
            ),
        )

    fun GameTestDriver.enteredFrom(entity: EntityId): Map<Zone, Boolean> =
        listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.EXILE).associateWith { zone ->
            val single = holds(TriggeringEntityEnteredOrWasCastFrom(zone), entity)
            holds(AnyEnteredOrWasCastFrom(zone), entity) shouldBe single
            single
        }

    fun only(zone: Zone) = listOf(Zone.HAND, Zone.LIBRARY, Zone.GRAVEYARD, Zone.EXILE).associateWith { it == zone }

    test("a permanent put onto the battlefield from a library entered from a library") {
        val d = driver()
        val bears = d.moveToBattlefield(d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears"))
        d.state.getEntity(bears)?.get<EnteredFromZoneComponent>()?.zone shouldBe Zone.LIBRARY
        d.enteredFrom(bears) shouldBe only(Zone.LIBRARY)
    }

    test("graveyard, exile and hand entries each record their own zone") {
        val d = driver()
        val fromGraveyard = d.moveToBattlefield(d.putCardInGraveyard(d.player1, "Grizzly Bears"))
        val fromExile = d.moveToBattlefield(d.putCardInExile(d.player1, "Grizzly Bears"))
        val fromHand = d.moveToBattlefield(d.putCardInHand(d.player1, "Grizzly Bears"))
        d.enteredFrom(fromGraveyard) shouldBe only(Zone.GRAVEYARD)
        d.enteredFrom(fromExile) shouldBe only(Zone.EXILE)
        d.enteredFrom(fromHand) shouldBe only(Zone.HAND)
    }

    test("leaving the battlefield strips the marker; the next entry records only its own zone") {
        val d = driver()
        val bears = d.moveToBattlefield(d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears"))
        d.replaceState(zones.moveToZone(d.state, bears, Zone.GRAVEYARD).state)
        d.state.getEntity(bears)?.get<EnteredFromZoneComponent>() shouldBe null

        d.moveToBattlefield(bears)
        d.enteredFrom(bears) shouldBe only(Zone.GRAVEYARD)
    }

    test("a land played from hand entered from a hand") {
        val d = driver()
        val forest = d.putCardInHand(d.player1, "Forest")
        d.playLand(d.player1, forest).error shouldBe null
        d.state.getEntity(forest)?.get<EnteredFromZoneComponent>()?.zone shouldBe Zone.HAND
        d.enteredFrom(forest) shouldBe only(Zone.HAND)
    }

    test("a permanent that never changed zones carries no provenance") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.enteredFrom(bears).values.any { it } shouldBe false
    }

    test("provenance is tracked only for the zones a permanent can come from") {
        shouldThrow<IllegalArgumentException> { TriggeringEntityEnteredOrWasCastFrom(Zone.BATTLEFIELD) }
        shouldThrow<IllegalArgumentException> { AnyEnteredOrWasCastFrom(Zone.STACK) }
    }
})
