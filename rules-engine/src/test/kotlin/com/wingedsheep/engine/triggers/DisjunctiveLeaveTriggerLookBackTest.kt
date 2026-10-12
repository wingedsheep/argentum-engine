package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * A `Triggers.or` disjunction that names a dies / leaves-the-battlefield atom must fire on the
 * look-back path (CR 603.10a: leaves-the-battlefield abilities look back in time) exactly as the bare
 * atom would — once per departure, never for a destination none of its atoms names.
 */
class DisjunctiveLeaveTriggerLookBackTest : FunSpec({
    val diesOrExiled = card("Test Dies Or Exiled") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.or(Triggers.self.dies(), Triggers.self.leaves(to = Zone.EXILE))
            effect = Effects.GainLife(1)
        }
    }
    val diesOrLeaves = card("Test Dies Or Leaves") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.or(Triggers.self.dies(), Triggers.self.leaves())
            effect = Effects.GainLife(1)
        }
    }
    val entersOrDies = card("Test Enters Or Dies") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.or(Triggers.self.enters(), Triggers.self.dies())
            effect = Effects.GainLife(1)
        }
    }
    val anyArtifactDiesOrExiled = card("Test Any Artifact Dies Or Exiled") {
        typeLine = "Artifact"
        triggeredAbility {
            val artifact = GameObjectFilter.Artifact.youControl()
            trigger = Triggers.or(Triggers.a(artifact).dies(), Triggers.a(artifact).leaves(to = Zone.EXILE))
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(diesOrExiled, diesOrLeaves, entersOrDies, anyArtifactDiesOrExiled))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
    }

    fun departure(state: GameState, id: EntityId, owner: EntityId, destination: Zone) =
        ZoneChangeEvent(
            entityId = id,
            entityName = state.getEntity(id)!!.get<CardComponent>()!!.name,
            fromZone = Zone.BATTLEFIELD,
            toZone = destination,
            ownerId = owner,
            lastKnown = EntitySnapshot.fromProjection(id, state).copy(
                cardDefinitionId = state.getEntity(id)?.get<CardComponent>()?.cardDefinitionId,
            ),
        )

    /** Moves [id] off the battlefield to [destination] and returns the triggers its departure detects. */
    fun detectOnDeparture(d: GameTestDriver, id: EntityId, destination: Zone) =
        d.state.let { before ->
            val event = departure(before, id, d.player1, destination)
            val after = before.moveToZone(id, before.logicalZone(id)!!, ZoneKey(d.player1, destination))
            d.services.triggerDetector.detectTriggers(after, listOf(event)).filter { it.sourceId == id }
        }

    test("self dies-or-exiled fires once when the permanent dies") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, diesOrExiled.name)
        detectOnDeparture(d, id, Zone.GRAVEYARD).size shouldBe 1
    }

    test("self dies-or-exiled fires once when the permanent is exiled") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, diesOrExiled.name)
        detectOnDeparture(d, id, Zone.EXILE).size shouldBe 1
    }

    test("self dies-or-exiled does not fire when the permanent is returned to hand") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, diesOrExiled.name)
        detectOnDeparture(d, id, Zone.HAND).shouldBeEmpty()
    }

    test("self dies-or-leaves fires exactly once on death even though both atoms match") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, diesOrLeaves.name)
        detectOnDeparture(d, id, Zone.GRAVEYARD).size shouldBe 1
    }

    test("self dies-or-leaves fires once on a bounce through its generic leaves atom") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, diesOrLeaves.name)
        detectOnDeparture(d, id, Zone.HAND).size shouldBe 1
    }

    test("a dies atom disjoined with a non-zone-exit atom still fires on death") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, entersOrDies.name)
        detectOnDeparture(d, id, Zone.GRAVEYARD).size shouldBe 1
        val other = d.putCreatureOnBattlefield(d.player1, entersOrDies.name)
        detectOnDeparture(d, other, Zone.EXILE).shouldBeEmpty()
    }

    test("an ANY-bound disjunction sees its own exile through the filter") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, anyArtifactDiesOrExiled.name)
        detectOnDeparture(d, id, Zone.EXILE).size shouldBe 1
        val other = d.putPermanentOnBattlefield(d.player1, anyArtifactDiesOrExiled.name)
        detectOnDeparture(d, other, Zone.HAND).shouldBeEmpty()
    }
})
