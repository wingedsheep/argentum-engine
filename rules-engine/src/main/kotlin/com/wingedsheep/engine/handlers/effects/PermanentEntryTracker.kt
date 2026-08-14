package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.player.EnteredPermanentRecord
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.EntityId

/**
 * Records the entry of a permanent into the per-player, per-turn entry log
 * ([PermanentsEnteredUnderControlThisTurnComponent]) that backs every "an X entered the
 * battlefield under your control this turn" reader — the Celebration ability word (WOE), the
 * land-entry count (Bioengineered Future), the card-type-entered condition (Mechan Shieldmate)
 * and the subtype-entry count (Geralf, the Fleshwright).
 *
 * Cleared at end of turn by [com.wingedsheep.engine.core.CleanupPhaseManager].
 *
 * Two sanctioned recording paths keep this log in sync:
 *  - The standard zone-change pipeline ([ZoneTransitionService.moveToZone]) calls [record]
 *    itself, right after wiring the entering permanent's controller.
 *  - Every *other* (ad-hoc) battlefield insertion — token creation, land play, permanent-
 *    spell resolution, returns from linked exile, etc. — must go through
 *    [BattlefieldEntry.place] rather than calling `state.addToZone(...)` directly.
 *
 * [record] appends **one entry per call**, deliberately without deduplicating on entity id: a
 * permanent that leaves the battlefield and returns is a new object (CR 400.7) and its second
 * entry is a second event that "a permanent entered the battlefield" abilities see. Both
 * sanctioned recording paths call [record] exactly once per ETB; the log would skew if a future
 * call site introduced double-recording.
 *
 * Types and subtypes are read from the **projected** state (post-layer), not the printed type
 * line, so a permanent that is an artifact by continuous effect at the moment of entry is
 * recorded as having entered as an artifact. The record is permanent for the rest of the turn —
 * once logged, it stays even if the permanent later leaves the battlefield, changes type, or
 * changes controller.
 */
object PermanentEntryTracker {

    /**
     * Record that [entityId] just entered the battlefield under [controllerId]. The
     * recorded card types are read from the projected state, which the caller is
     * responsible for having brought up to date (i.e. [entityId] must already be on the
     * battlefield with its identity components in place).
     */
    fun record(state: GameState, controllerId: EntityId, entityId: EntityId): GameState {
        // Object-identity stamp (CR 400.7): every battlefield entry funnels through here, so
        // this is where the entering permanent gets its entry timestamp. Delayed triggers that
        // track a specific permanent (CR 603.7c) snapshot this value and compare it at
        // resolution — a mismatch means the entity left and re-entered as a new object.
        // Stamp, then tick: resolutions do NOT advance the global timestamp (only puts on the
        // stack, land plays, and similar actions do), so two back-to-back resolutions can share
        // a value — a blink resolving at the same timestamp as the original entry would collide
        // with the snapshot. Ticking here makes every entry stamp unique unconditionally.
        val stamped = state.updateEntity(entityId) { container ->
            container.with(BattlefieldEntryTimestampComponent(state.timestamp))
        }.tick()
        val cardTypes = projectedCardTypes(stamped, entityId)
        if (cardTypes.isEmpty()) return stamped
        val subtypes = stamped.projectedState.getSubtypes(entityId)
        return stamped.updateEntity(controllerId) { container ->
            val existing = container.get<PermanentsEnteredUnderControlThisTurnComponent>()
                ?: PermanentsEnteredUnderControlThisTurnComponent()
            container.with(
                PermanentsEnteredUnderControlThisTurnComponent(
                    existing.entries + EnteredPermanentRecord(entityId, cardTypes, subtypes)
                )
            )
        }
    }

    private fun projectedCardTypes(state: GameState, entityId: EntityId): Set<CardType> {
        val typeNames = state.projectedState.getTypes(entityId)
        if (typeNames.isEmpty()) return emptySet()
        val byName = CardType.entries.associateBy { it.name }
        return typeNames.mapNotNull { byName[it] }.toSet()
    }
}
