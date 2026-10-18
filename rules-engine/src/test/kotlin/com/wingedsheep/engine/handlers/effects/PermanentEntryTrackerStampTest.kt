package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.layers.LayerTimestamps
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pins the uniqueness invariant of the battlefield-entry object-identity stamp (CR 400.7 /
 * 603.7c): [PermanentEntryTracker.record] stamps the entering permanent with the current
 * global timestamp and then ticks it, so distinct entries always get distinct stamps —
 * even when nothing else (a cast, a land play) advances the timestamp in between.
 * Resolutions themselves don't tick, so without the tick-in-record two back-to-back
 * resolutions could stamp an entry and a blink re-entry with the same value, and the
 * delayed-trigger snapshot comparison in WarpExileExecutor would falsely match.
 */
class PermanentEntryTrackerStampTest : FunSpec({

    fun entryStamp(state: GameState, id: EntityId): Long? =
        state.getEntity(id)?.get<BattlefieldEntryTimestampComponent>()?.timestamp

    test("two entries recorded with no tick in between get distinct stamps") {
        val controller = EntityId.generate()
        val first = EntityId.generate()
        val second = EntityId.generate()
        var state = GameState()
            .withEntity(controller, ComponentContainer())
            .withEntity(first, ComponentContainer())
            .withEntity(second, ComponentContainer())

        state = PermanentEntryTracker.record(state, controller, first)
        state = PermanentEntryTracker.record(state, controller, second)

        entryStamp(state, second) shouldNotBe entryStamp(state, first)
    }

    test("a re-entry of the same entity gets a fresh stamp") {
        val controller = EntityId.generate()
        val permanent = EntityId.generate()
        var state = GameState()
            .withEntity(controller, ComponentContainer())
            .withEntity(permanent, ComponentContainer())

        state = PermanentEntryTracker.record(state, controller, permanent)
        val originalStamp = entryStamp(state, permanent)!!

        // Leave the battlefield (the zone-change pipeline strips the stamp), then re-enter
        // immediately — the same resolution, no tick from any cast or land play.
        state = state.updateEntity(permanent) { it.without<BattlefieldEntryTimestampComponent>() }
        state = PermanentEntryTracker.record(state, controller, permanent)

        entryStamp(state, permanent)!! shouldBeGreaterThan originalStamp
    }

    fun layerStamp(state: GameState, id: EntityId): Long? =
        state.getEntity(id)?.get<TimestampComponent>()?.timestamp

    test("entry also sets the layer timestamp, which a renewal moves without touching the entry stamp") {
        val controller = EntityId.generate()
        val permanent = EntityId.generate()
        var state = GameState()
            .withEntity(controller, ComponentContainer())
            .withEntity(permanent, ComponentContainer())

        state = PermanentEntryTracker.record(state, controller, permanent)
        val entry = entryStamp(state, permanent)!!
        layerStamp(state, permanent) shouldBe entry

        // CR 613.7e-g: becoming attached, turning face up/down or transforming renews only the
        // layer timestamp; the battlefield object (CR 400.7) is the same one.
        state = LayerTimestamps.renew(state, permanent)
        layerStamp(state, permanent)!! shouldBeGreaterThan entry
        entryStamp(state, permanent) shouldBe entry
        state.timestamp shouldBeGreaterThan layerStamp(state, permanent)!!
    }
})
