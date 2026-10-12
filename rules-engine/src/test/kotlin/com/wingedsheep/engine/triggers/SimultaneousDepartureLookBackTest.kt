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
 * CR 603.10a: leaves-the-battlefield abilities look back in time, so a permanent that leaves the
 * battlefield alongside others still sees them leave — whichever zone either one went to, not only
 * when both died.
 */
class SimultaneousDepartureLookBackTest : FunSpec({
    val otherArtifact = GameObjectFilter.Artifact.youControl()
    val watchesOthersDieOrExiled = card("Test Other Artifact Dies Or Exiled") {
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.or(
                Triggers.another(otherArtifact).dies(),
                Triggers.another(otherArtifact).leaves(to = Zone.EXILE)
            )
            effect = Effects.GainLife(1)
        }
    }
    val watchesOthersDie = card("Test Other Artifact Dies") {
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.another(otherArtifact).dies()
            effect = Effects.GainLife(1)
        }
    }
    val watchesOthersLeave = card("Test Other Artifact Leaves") {
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.another(otherArtifact).leaves()
            effect = Effects.GainLife(1)
        }
    }
    val plainArtifact = card("Test Plain Artifact") {
        typeLine = "Artifact"
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(watchesOthersDieOrExiled, watchesOthersDie, watchesOthersLeave, plainArtifact))
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

    /** Moves every permanent in [moves] off the battlefield at once; returns the triggers [watcher] fires. */
    fun detectTogether(d: GameTestDriver, watcher: EntityId, moves: List<Pair<EntityId, Zone>>) =
        d.state.let { before ->
            val events = moves.map { (id, zone) -> departure(before, id, d.player1, zone) }
            val after = moves.fold(before) { s, (id, zone) ->
                s.moveToZone(id, s.logicalZone(id)!!, ZoneKey(d.player1, zone))
            }
            d.services.triggerDetector.detectTriggers(after, events).filter { it.sourceId == watcher }
        }

    test("a watcher exiled together with another artifact sees it exiled") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, watchesOthersDieOrExiled.name)
        val other = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        val fired = detectTogether(d, watcher, listOf(watcher to Zone.EXILE, other to Zone.EXILE))
        fired.size shouldBe 1
        fired.single().triggerContext.triggeringEntityId shouldBe other
    }

    test("a watcher exiled while another artifact dies sees the death") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, watchesOthersDie.name)
        val other = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        detectTogether(d, watcher, listOf(watcher to Zone.EXILE, other to Zone.GRAVEYARD)).size shouldBe 1
    }

    test("a watcher that dies while another artifact is exiled sees the exile") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, watchesOthersDieOrExiled.name)
        val other = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        detectTogether(d, watcher, listOf(watcher to Zone.GRAVEYARD, other to Zone.EXILE)).size shouldBe 1
    }

    test("a bounced watcher sees another artifact bounced with it through a generic leaves trigger") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, watchesOthersLeave.name)
        val other = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        detectTogether(d, watcher, listOf(watcher to Zone.HAND, other to Zone.HAND)).size shouldBe 1
    }

    test("a departure the trigger doesn't name still doesn't fire it") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, watchesOthersDie.name)
        val other = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        detectTogether(d, watcher, listOf(watcher to Zone.EXILE, other to Zone.EXILE)).shouldBeEmpty()
    }

    test("simultaneous deaths still look back once per other death") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, watchesOthersDie.name)
        val a = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        val b = d.putPermanentOnBattlefield(d.player1, plainArtifact.name)
        detectTogether(d, watcher, listOf(watcher to Zone.GRAVEYARD, a to Zone.GRAVEYARD, b to Zone.GRAVEYARD))
            .size shouldBe 2
    }
})
