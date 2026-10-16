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
 * A self-bound dies / leaves-the-battlefield trigger fires on its own departure only when the
 * departure matches the whole pattern: a `Triggers.self.matching(filter)` narrowing ("when this
 * creature dies, if it was enchanted" — Gunner Conscript) is answered from the zone change's
 * last-known snapshot, and `excludeTo` ("leaves without dying") is honoured.
 */
class SelfFilteredDepartureTriggerTest : FunSpec({
    val diesIfArtifact = card("Test Dies If Artifact") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.matching(GameObjectFilter.Artifact).dies()
            effect = Effects.GainLife(1)
        }
    }
    val artifactDiesIfArtifact = card("Test Artifact Dies If Artifact") {
        typeLine = "Artifact Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.matching(GameObjectFilter.Artifact).dies()
            effect = Effects.GainLife(1)
        }
    }
    val leavesIfArtifact = card("Test Leaves If Artifact") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.matching(GameObjectFilter.Artifact).leaves()
            effect = Effects.GainLife(1)
        }
    }
    val leavesWithoutDying = card("Test Leaves Without Dying") {
        typeLine = "Creature"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.leaves(excludeTo = Zone.GRAVEYARD)
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(
            TestCards.all + listOf(diesIfArtifact, artifactDiesIfArtifact, leavesIfArtifact, leavesWithoutDying)
        )
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

    test("a self dies trigger narrowed by matching() doesn't fire when its own death fails the filter") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, diesIfArtifact.name)
        detectOnDeparture(d, id, Zone.GRAVEYARD).shouldBeEmpty()
    }

    test("a self dies trigger narrowed by matching() fires when its own death matches the filter") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, artifactDiesIfArtifact.name)
        detectOnDeparture(d, id, Zone.GRAVEYARD).size shouldBe 1
    }

    test("a self leaves trigger narrowed by matching() doesn't fire when the departure fails the filter") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, leavesIfArtifact.name)
        detectOnDeparture(d, id, Zone.HAND).shouldBeEmpty()
    }

    test("a self leaves-without-dying trigger ignores its own death but fires on a bounce") {
        val d = driver()
        val dying = d.putCreatureOnBattlefield(d.player1, leavesWithoutDying.name)
        detectOnDeparture(d, dying, Zone.GRAVEYARD).shouldBeEmpty()
        val bounced = d.putCreatureOnBattlefield(d.player1, leavesWithoutDying.name)
        detectOnDeparture(d, bounced, Zone.HAND).size shouldBe 1
    }
})
