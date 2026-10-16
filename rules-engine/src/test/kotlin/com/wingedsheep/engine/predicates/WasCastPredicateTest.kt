package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

/**
 * `StatePredicate.WasCast(fromZone)` — "a spell that was cast [from <zone>]", read off the
 * `SpellOnStackComponent.castFromZone` the engine stamps at cast time. A copy put onto the stack
 * isn't cast (CR 707.10) and carries no cast origin. Pinned here: the zone-free reading matches
 * every cast spell and no copy, the zone reading matches only that zone and `Not` gives the
 * "wasn't cast" phrasings (Errant, Street Artist; Wash Away).
 */
class WasCastPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val player = EntityId.generate()

    val fromHand = EntityId.generate()
    val fromGraveyard = EntityId.generate()
    val copy = EntityId.generate()
    val all = listOf(fromHand, fromGraveyard, copy)

    val state = listOf(
        fromHand to Zone.HAND,
        fromGraveyard to Zone.GRAVEYARD,
        copy to null,
    ).fold(GameState().withEntity(player, ComponentContainer())) { s, (id, zone) ->
        s.withEntity(id, ComponentContainer().with(SpellOnStackComponent(casterId = player, castFromZone = zone)))
            .addToZone(ZoneKey(player, Zone.STACK), id)
    }

    fun matching(predicate: StatePredicate): List<EntityId> {
        val filter = GameObjectFilter.Any.withStatePredicate(predicate)
        return all.filter { evaluator.matches(state, state.projectedState, it, filter, PredicateContext(controllerId = player)) }
    }

    test("with no zone, every cast spell matches and a copy doesn't") {
        matching(StatePredicate.WasCast()).shouldContainExactlyInAnyOrder(fromHand, fromGraveyard)
    }

    test("\"wasn't cast\" matches only the copy") {
        matching(StatePredicate.Not(StatePredicate.WasCast())).shouldContainExactlyInAnyOrder(copy)
    }

    test("with a zone, only spells cast from that zone match") {
        matching(StatePredicate.WasCast(Zone.HAND)).shouldContainExactlyInAnyOrder(fromHand)
        matching(StatePredicate.WasCast(Zone.GRAVEYARD)).shouldContainExactlyInAnyOrder(fromGraveyard)
    }

    test("\"wasn't cast from a hand\" includes the copy, which wasn't cast at all") {
        matching(StatePredicate.Not(StatePredicate.WasCast(Zone.HAND)))
            .shouldContainExactlyInAnyOrder(fromGraveyard, copy)
    }
})
