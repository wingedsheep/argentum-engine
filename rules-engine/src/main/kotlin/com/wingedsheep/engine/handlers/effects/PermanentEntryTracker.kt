package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.FinishResolvingAbilityContinuation
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.FinishResolvingSpellContinuation
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.innermostResolvingFrame
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.EnteredPermanentRecord
import com.wingedsheep.engine.state.components.player.NontokenPermanentsPutOntoBattlefieldThisTurnComponent
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

    /** Entry filters and gates read only components and counts, never a printed script. */
    private val entryRiderEvaluator = PredicateEvaluator(cardRegistry = null)

    /**
     * Record that [entityId] just entered the battlefield under [controllerId]. The
     * recorded card types are read from the projected state, which the caller is
     * responsible for having brought up to date (i.e. [entityId] must already be on the
     * battlefield with its identity components in place). [simultaneousEntryGroup] marks an
     * entry that is part of one simultaneous event with other permanents. [putBy] is the player
     * who put the permanent there when the caller knows it (a resolving permanent spell's caster);
     * otherwise it is read off the innermost resolving spell or ability, falling back to
     * [controllerId] for a move with nothing resolving (a land play).
     */
    fun record(
        state: GameState,
        controllerId: EntityId,
        entityId: EntityId,
        simultaneousEntryGroup: String? = null,
        putBy: EntityId? = null
    ): GameState {
        // Object-identity stamp (CR 400.7): every battlefield entry funnels through here, so
        // this is where the entering permanent gets its entry timestamp. Delayed triggers that
        // track a specific permanent (CR 603.7c) snapshot this value and compare it at
        // resolution — a mismatch means the entity left and re-entered as a new object.
        // Stamp, then tick: resolutions do NOT advance the global timestamp (only puts on the
        // stack, land plays, and similar actions do), so two back-to-back resolutions can share
        // a value — a blink resolving at the same timestamp as the original entry would collide
        // with the snapshot. Ticking here makes every entry stamp unique unconditionally.
        // The same moment is the permanent's first layer timestamp (CR 613.7d), which its static
        // abilities' continuous effects sort by (CR 613.7a); see LayerTimestamps for its renewals.
        val stamped = state.updateEntity(entityId) { container ->
            container.with(BattlefieldEntryTimestampComponent(state.timestamp, simultaneousEntryGroup))
                .with(TimestampComponent(state.timestamp))
        }.tick().let { ticked ->
            val putter = putBy ?: resolvingController(ticked) ?: controllerId
            // "If a player would put a land onto the battlefield, that player instead puts it onto
            // the battlefield, then …" (Land Equilibrium) — queued here because every entry funnels
            // through this method and this is where the putting player is known.
            EntryRiderReplacements.queue(recordPut(ticked, putter, entityId), entityId, putter, entryRiderEvaluator)
        }
        val cardTypes = projectedCardTypes(stamped, entityId)
        if (cardTypes.isEmpty()) return stamped
        val subtypes = stamped.projectedState.getSubtypes(entityId)
        return stamped.updateEntity(controllerId) { container ->
            val existing = container.get<PermanentsEnteredUnderControlThisTurnComponent>()
                ?: PermanentsEnteredUnderControlThisTurnComponent()
            container.with(
                PermanentsEnteredUnderControlThisTurnComponent(
                    existing.entries + EnteredPermanentRecord(
                        entityId,
                        cardTypes,
                        subtypes,
                        isToken = stamped.getEntity(entityId)?.has<TokenComponent>() == true,
                    )
                )
            )
        }
    }

    /**
     * Credit [actor] with putting a nontoken permanent onto the battlefield this turn
     * ([NontokenPermanentsPutOntoBattlefieldThisTurnComponent]). A token's entry is its creation,
     * not a put of a nontoken permanent, so it is skipped.
     */
    private fun recordPut(state: GameState, actor: EntityId, entityId: EntityId): GameState {
        if (state.getEntity(entityId)?.has<TokenComponent>() != false) return state
        if (state.getEntity(actor) == null) return state
        return state.updateEntity(actor) { container ->
            val count = container.get<NontokenPermanentsPutOntoBattlefieldThisTurnComponent>()?.count ?: 0
            container.with(NontokenPermanentsPutOntoBattlefieldThisTurnComponent(count + 1))
        }
    }

    /**
     * The controller of the innermost spell or ability now resolving — the player whose effect is
     * putting things onto the battlefield.
     */
    private fun resolvingController(state: GameState): EntityId? =
        when (val frame = state.innermostResolvingFrame()) {
            is FinishResolvingSpellContinuation -> frame.spellComponent.casterId
            is FinishResolvingAbilityContinuation ->
                TargetResolutionUtils.stackObjectController(state, frame.abilityObject.entityId)
            else -> null
        }

    private fun projectedCardTypes(state: GameState, entityId: EntityId): Set<CardType> {
        val typeNames = state.projectedState.getTypes(entityId)
        if (typeNames.isEmpty()) return emptySet()
        val byName = CardType.entries.associateBy { it.name }
        return typeNames.mapNotNull { byName[it] }.toSet()
    }
}
