package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.EnteredPermanentRecord
import com.wingedsheep.engine.state.components.player.FaceDownPermanentsEnteredThisTurnComponent
import com.wingedsheep.engine.state.components.player.LandsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentTypesEnteredBattlefieldThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.EntityId

/**
 * Records the entry of a permanent for per-player, per-turn "an X entered the battlefield
 * under your control this turn" tracking (e.g. Mechan Shieldmate).
 *
 * Cleared at end of turn by [com.wingedsheep.engine.core.CleanupPhaseManager].
 *
 * Two sanctioned recording paths keep this tracker in sync:
 *  - The standard zone-change pipeline ([ZoneTransitionService.moveToZone]) calls [record]
 *    itself, right after wiring the entering permanent's controller.
 *  - Every *other* (ad-hoc) battlefield insertion — token creation, land play, permanent-
 *    spell resolution, returns from linked exile, etc. — must go through
 *    [BattlefieldEntry.place] rather than calling `state.addToZone(...)` directly.
 *
 * The type-set tracker ([PermanentTypesEnteredBattlefieldThisTurnComponent]) merges into a
 * [Set], so it is safe (idempotent) if an entry is ever recorded twice. The land-count
 * tracker ([LandsEnteredUnderControlThisTurnComponent]) is **not** idempotent — it bumps
 * unconditionally when the entering permanent's types include `LAND`. Both sanctioned
 * recording paths call [record] exactly once per ETB; the count would skew if a future
 * call site introduced double-recording.
 *
 * Types are read from the **projected** state (post-layer), not the printed type line, so
 * a permanent that is an artifact by continuous effect at the moment of entry is recorded
 * as having entered as an artifact. The record itself is permanent for the rest of the
 * turn — once recorded, it stays true even if the permanent later leaves the battlefield
 * or changes type.
 */
object PermanentEntryTracker {

    /**
     * Record that [entityId] just entered the battlefield under [controllerId]. The
     * recorded card types are read from the projected state, which the caller is
     * responsible for having brought up to date (i.e. [entityId] must already be on the
     * battlefield with its identity components in place).
     */
    fun record(state: GameState, controllerId: EntityId, entityId: EntityId): GameState {
        // Face-down entry is tracked independently of visible card types: it's the *manner* of
        // entry that matters ("a permanent entered the battlefield face down under your control
        // this turn"), so it must hold even for a hypothetical entry with no projected types.
        val enteredFaceDown = state.getEntity(entityId)?.has<FaceDownComponent>() == true
        val cardTypes = projectedCardTypes(state, entityId)
        if (cardTypes.isEmpty() && !enteredFaceDown) return state
        val subtypes = state.projectedState.getSubtypes(entityId)
        return state.updateEntity(controllerId) { container ->
            // Bump the per-player face-down entry count once for this ETB. Counts (not a marker)
            // so future "for each permanent that entered face down this turn" cards compose.
            val faceDownTracked = if (enteredFaceDown) {
                val existing = container.get<FaceDownPermanentsEnteredThisTurnComponent>()
                    ?: FaceDownPermanentsEnteredThisTurnComponent()
                container.with(FaceDownPermanentsEnteredThisTurnComponent(existing.count + 1))
            } else {
                container
            }
            if (cardTypes.isEmpty()) return@updateEntity faceDownTracked
            val typeMerged = run {
                val existing = faceDownTracked.get<PermanentTypesEnteredBattlefieldThisTurnComponent>()
                    ?: PermanentTypesEnteredBattlefieldThisTurnComponent()
                val merged = existing.cardTypes + cardTypes
                if (merged == existing.cardTypes) faceDownTracked
                else faceDownTracked.with(PermanentTypesEnteredBattlefieldThisTurnComponent(merged))
            }
            // Per-permanent entry list, keyed by entityId so a (theoretical) double-record is
            // idempotent. Backs subtype-keyed "for each [type] that entered this turn" counts.
            val perPermanentMerged = run {
                val existing = typeMerged.get<PermanentsEnteredUnderControlThisTurnComponent>()
                    ?: PermanentsEnteredUnderControlThisTurnComponent()
                if (existing.entries.any { it.entityId == entityId }) typeMerged
                else typeMerged.with(
                    PermanentsEnteredUnderControlThisTurnComponent(
                        existing.entries + EnteredPermanentRecord(entityId, subtypes)
                    )
                )
            }
            // Lands need a *count*, not just presence, for "for each land that entered
            // this turn" dynamic amounts (Bioengineered Future). Two unrelated lands
            // entering both bump the counter; the set-based type tracker above sees them
            // as a single LAND entry.
            if (CardType.LAND in cardTypes) {
                val landExisting = perPermanentMerged.get<LandsEnteredUnderControlThisTurnComponent>()
                    ?: LandsEnteredUnderControlThisTurnComponent()
                perPermanentMerged.with(LandsEnteredUnderControlThisTurnComponent(landExisting.count + 1))
            } else {
                perPermanentMerged
            }
        }
    }

    private fun projectedCardTypes(state: GameState, entityId: EntityId): Set<CardType> {
        val typeNames = state.projectedState.getTypes(entityId)
        if (typeNames.isEmpty()) return emptySet()
        val byName = CardType.entries.associateBy { it.name }
        return typeNames.mapNotNull { byName[it] }.toSet()
    }
}
