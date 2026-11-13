package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.ZoneTransitionCause
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.linkedexile.MoveUntilSourceLeavesExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class MoveUntilSourceLeavesTest : ScenarioTestBase() {
    private val executor = MoveUntilSourceLeavesExecutor(zones)

    init {
        for (origin in listOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.HAND)) {
            test("returns to the previous $origin zone and survives state serialization") {
                val game = board()
                val source = game.findPermanent("Sol Ring")!!
                val victim = game.findPermanent("Grizzly Bears")!!
                val start = if (origin == Zone.BATTLEFIELD) game.state else
                    zones.moveToZone(game.state, victim, origin).state
                val context = context(start, source)
                val moved = executor.execute(start, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), context)
                val json = Json {
                    serializersModule = com.wingedsheep.engine.core.engineSerializersModule
                    allowStructuredMapKeys = true
                }
                val restored = json.decodeFromString<GameState>(json.encodeToString(moved.state))
                val result = zones.moveToZone(restored, source, Zone.GRAVEYARD)
                result.state.getZone(ZoneKey(game.player2Id, origin)) shouldContain victim
                result.state.zoneReturns.size shouldBe 0
                result.state.stack.size shouldBe 0
                result.events.filterIsInstance<ZoneChangeEvent>().last().transitionCause shouldBe ZoneTransitionCause.DURATION_RETURN
            }
        }

        test("returns under its owner's control as a fresh object without old counters") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val start = game.state.updateEntity(victim) { c ->
                c.with(c.get<CardComponent>()!!.copy(ownerId = game.player1Id))
                    .with(OwnerComponent(game.player1Id))
                    .with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
            }
            val moved = executor.execute(start, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), context(start, source))
            val result = zones.moveToZone(moved.state, source, Zone.HAND)
            result.state.projectedState.getController(victim) shouldBe game.player1Id
            result.state.getEntity(victim)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE).let { it ?: 0 } shouldBe 0
            result.state.isCurrentObject(start.objectRef(victim)!!) shouldBe false
        }

        test("a source that leaves and returns cannot exile through its old ability") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val oldContext = context(game.state, source)
            val gone = zones.moveToZone(game.state, source, Zone.EXILE).state
            val back = zones.moveToZone(gone, source, Zone.BATTLEFIELD).state
            val result = executor.execute(back, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), oldContext)
            result.state.getBattlefield() shouldContain victim
            result.state.zoneReturns.size shouldBe 0
            result.events.size shouldBe 0
        }

        test("a target that leaves exile and is exiled again is not returned") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val moved = executor.execute(game.state, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), context(game.state, source))
            val gone = zones.moveToZone(moved.state, victim, Zone.HAND).state
            val reexiled = zones.moveToZone(gone, victim, Zone.EXILE).state
            val result = zones.moveToZone(reexiled, source, Zone.GRAVEYARD)
            result.state.getExile(game.player2Id) shouldContain victim
            result.state.zoneReturns.size shouldBe 0
        }

        test("phasing out before resolution does not end the duration or prevent the move") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val captured = context(game.state, source)
            val phased = game.state.updateEntity(source) {
                it.with(com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent(game.player1Id))
            }
            val moved = executor.execute(phased, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), captured)
            moved.state.getExile(game.player2Id) shouldContain victim
            moved.state.zoneReturns.size shouldBe 1
            val phasedIn = moved.state.updateEntity(source) {
                it.without<com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent>()
            }
            val result = zones.moveToZone(phasedIn, source, Zone.HAND)
            result.state.getBattlefield() shouldContain victim
        }

        test("leaving the game returns an opponent's card even without zone-change triggers") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val moved = executor.execute(game.state, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), context(game.state, source))
            val result = com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameProcessor.process(
                zones,
                moved.state, game.player1Id, com.wingedsheep.engine.core.GameEndReason.CONCESSION,
                services.finishAbandonedResolution
            )
            result.state.getBattlefield() shouldContain victim
            result.state.zoneReturns.size shouldBe 0
        }

        test("the temporary destination can be a hand instead of exile") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val moved = executor.execute(game.state, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.HAND), context(game.state, source))
            moved.state.getHand(game.player2Id) shouldContain victim
            zones.moveToZone(moved.state, source, Zone.GRAVEYARD).state.getBattlefield() shouldContain victim
        }

        test("return applies the creature's enters-with-counters replacement") {
            val game = board("Triskelion")
            val source = game.findPermanent("Sol Ring")!!
            val victim = game.findPermanent("Triskelion")!!
            val moved = executor.execute(game.state, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), context(game.state, source))
            val result = zones.moveToZone(moved.state, source, Zone.GRAVEYARD)
            result.state.getEntity(victim)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }

        test("moving the source itself ends the duration during the initial move") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val result = executor.execute(game.state, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(source), Zone.EXILE), context(game.state, source))
            result.state.getBattlefield() shouldContain source
            result.state.zoneReturns.size shouldBe 0
            result.events.filterIsInstance<ZoneChangeEvent>().map { it.toZone } shouldBe listOf(Zone.EXILE, Zone.BATTLEFIELD)
        }

        test("multiple objects return when one source leaves, even though it has no return ability") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            var state = game.state
            for (name in listOf("Grizzly Bears", "Hill Giant")) {
                val victim = game.findPermanent(name)!!
                state = executor.execute(state, MoveUntilSourceLeavesEffect(EffectTarget.SpecificEntity(victim), Zone.EXILE), context(state, source)).state
            }
            val result = zones.moveToZone(state, source, Zone.GRAVEYARD)
            for (name in listOf("Grizzly Bears", "Hill Giant")) result.state.getBattlefield() shouldContain game.findPermanent(name)!!
            result.state.zoneReturns.size shouldBe 0
        }

        test("another anchor bounds the duration: the source leaving changes nothing") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val anchor = game.findPermanent("Hill Giant")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val effect = MoveUntilSourceLeavesEffect(
                EffectTarget.SpecificEntity(victim), Zone.EXILE, anchor = EffectTarget.SpecificEntity(anchor)
            )
            val moved = executor.execute(game.state, effect, context(game.state, source))
            moved.state.getExile(game.player2Id) shouldContain victim
            moved.state.getEntity(source)?.get<LinkedExileComponent>() shouldBe null
            val sourceGone = zones.moveToZone(moved.state, source, Zone.GRAVEYARD).state
            sourceGone.getExile(game.player2Id) shouldContain victim
            val anchorGone = zones.moveToZone(sourceGone, anchor, Zone.GRAVEYARD).state
            anchorGone.getBattlefield() shouldContain victim
            anchorGone.zoneReturns.size shouldBe 0
        }

        test("an anchor already off the battlefield moves nothing") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val anchor = game.findPermanent("Hill Giant")!!
            val victim = game.findPermanent("Grizzly Bears")!!
            val gone = zones.moveToZone(game.state, anchor, Zone.GRAVEYARD).state
            val effect = MoveUntilSourceLeavesEffect(
                EffectTarget.SpecificEntity(victim), Zone.EXILE, anchor = EffectTarget.SpecificEntity(anchor)
            )
            val result = executor.execute(gone, effect, context(gone, source))
            result.state.getBattlefield() shouldContain victim
            result.state.zoneReturns.size shouldBe 0
        }
        // "Exile all <filter> until this leaves the battlefield" — the GroupRef form (Aligned Hedron Network).
        test("a group moves in one simultaneous move, whoever controls each member, and links every card") {
            val game = groupBoard()
            val source = game.findPermanent("Sol Ring")!!
            val giants = game.findAllPermanents("Hill Giant")
            val bears = game.findPermanent("Grizzly Bears")!!
            val moved = executor.execute(game.state, bigCreatures(), context(game.state, source))
            for (giant in giants) moved.state.getBattlefield().contains(giant) shouldBe false
            moved.state.getExile(game.player1Id) shouldContain giants.first { game.state.getEntity(it)!!.get<OwnerComponent>()!!.playerId == game.player1Id }
            moved.state.getExile(game.player2Id) shouldContain giants.first { game.state.getEntity(it)!!.get<OwnerComponent>()!!.playerId == game.player2Id }
            moved.state.getBattlefield() shouldContain bears
            moved.state.zoneReturns.size shouldBe 2
            moved.state.getEntity(source)!!.get<LinkedExileComponent>()!!.exiledIds.toSet() shouldBe giants.toSet()
            moved.events.filterIsInstance<ZoneChangeEvent>().count { it.toZone == Zone.EXILE } shouldBe 2
        }

        test("every member of the group returns at once, under its owner's control, when the source leaves") {
            val game = groupBoard()
            val source = game.findPermanent("Sol Ring")!!
            val giants = game.findAllPermanents("Hill Giant")
            val moved = executor.execute(game.state, bigCreatures(), context(game.state, source))
            val result = zones.moveToZone(moved.state, source, Zone.GRAVEYARD)
            for (giant in giants) {
                result.state.getBattlefield() shouldContain giant
                result.state.projectedState.getController(giant) shouldBe
                    game.state.getEntity(giant)!!.get<OwnerComponent>()!!.playerId
            }
            result.state.zoneReturns.size shouldBe 0
            val returns = result.events.filterIsInstance<ZoneChangeEvent>().filter { it.toZone == Zone.BATTLEFIELD }
            returns.map { it.transitionCause }.toSet() shouldBe setOf(ZoneTransitionCause.DURATION_RETURN)
            // One simultaneous entry event, not one card after another.
            returns.size shouldBe 2
            returns.map { it.simultaneousEntryGroup }.toSet().single() shouldNotBe null
        }

        test("a group member controlled by a non-owner returns under its owner's control") {
            val game = groupBoard()
            val source = game.findPermanent("Sol Ring")!!
            val stolen = game.findAllPermanents("Hill Giant").first {
                game.state.getEntity(it)!!.get<OwnerComponent>()!!.playerId == game.player2Id
            }
            val start = game.state.updateEntity(stolen) {
                it.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(game.player1Id))
            }
            start.projectedState.getController(stolen) shouldBe game.player1Id
            val moved = executor.execute(start, bigCreatures(), context(start, source))
            moved.state.getExile(game.player2Id) shouldContain stolen
            val result = zones.moveToZone(moved.state, source, Zone.GRAVEYARD)
            result.state.getBattlefield() shouldContain stolen
            result.state.projectedState.getController(stolen) shouldBe game.player2Id
        }

        test("a group effect from a source already gone moves nothing") {
            val game = groupBoard()
            val source = game.findPermanent("Sol Ring")!!
            val captured = context(game.state, source)
            val gone = zones.moveToZone(game.state, source, Zone.GRAVEYARD).state
            val result = executor.execute(gone, bigCreatures(), captured)
            for (giant in game.findAllPermanents("Hill Giant")) result.state.getBattlefield() shouldContain giant
            result.state.zoneReturns.size shouldBe 0
            result.events.size shouldBe 0
        }

        test("a group that includes the source exiles it with the rest, and everything returns immediately") {
            val game = groupBoard()
            val source = game.findPermanent("Sol Ring")!!
            val giants = game.findAllPermanents("Hill Giant")
            val effect = MoveUntilSourceLeavesEffect(
                EffectTarget.GroupRef(GroupFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature.powerAtLeast(3))),
                Zone.EXILE
            )
            val result = executor.execute(game.state, effect, context(game.state, source))
            result.state.getBattlefield() shouldContain source
            for (giant in giants) result.state.getBattlefield() shouldContain giant
            result.state.zoneReturns.size shouldBe 0
            result.events.filterIsInstance<ZoneChangeEvent>().count { it.toZone == Zone.EXILE } shouldBe 3
            result.events.filterIsInstance<ZoneChangeEvent>().count { it.toZone == Zone.BATTLEFIELD } shouldBe 3
        }

        test("an empty group moves nothing") {
            val game = board()
            val source = game.findPermanent("Sol Ring")!!
            val effect = MoveUntilSourceLeavesEffect(
                EffectTarget.GroupRef(GroupFilter(GameObjectFilter.Creature.powerAtLeast(5))), Zone.EXILE
            )
            val result = executor.execute(game.state, effect, context(game.state, source))
            result.state shouldBe game.state
            result.events.size shouldBe 0
        }
    }

    private fun bigCreatures() = MoveUntilSourceLeavesEffect(
        EffectTarget.GroupRef(GroupFilter(GameObjectFilter.Creature.powerAtLeast(3))), Zone.EXILE
    )

    private fun groupBoard() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Sol Ring")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island").build()

    private fun context(state: GameState, source: com.wingedsheep.sdk.model.EntityId) = EffectContext(
        sourceId = source,
        controllerId = state.projectedState.getController(source)!!,
        objectReferences = ObjectReferenceEnvironment(captured = true, source = state.objectRef(source))
    )

    private fun board(victim: String = "Grizzly Bears") = scenario().withPlayers()
        .withCardOnBattlefield(1, "Sol Ring")
        .withCardOnBattlefield(2, victim)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island").build()
}
