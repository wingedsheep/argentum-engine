package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * CR 611.2b — a continuous effect with a "for as long as …" duration ends the moment its
 * condition stops being true and does NOT restart if the condition later becomes true again
 * ("It doesn't start and immediately stop again, and it doesn't last forever").
 *
 * [com.wingedsheep.engine.mechanics.layers.StateProjector] already enforces these conditions
 * per projection frame, which gives the correct *instantaneous* view: the effect simply stops
 * applying while the condition is false. That half is reversible on its own — if the condition
 * flips back to true the gate re-applies the effect. This state-based check supplies the missing
 * half: once the condition has failed, the floating effect is physically removed from
 * [GameState.floatingEffects] (latched off), so no later projection can resurrect it.
 *
 * Concretely this fixes Old Man of the Sea: when the stolen creature is pumped past Old Man's
 * power its control reverts, and a temporary pump that later wears off must NOT re-steal it.
 *
 * Conditions handled — mirrors the gates in [com.wingedsheep.engine.mechanics.layers.StateProjector]:
 * - [Duration.WhileSourceTapped] / [Duration.WhileSourceTappedAndAffectedPowerAtMostSource]:
 *   ends when the source leaves the battlefield or untaps. The power variant additionally drops
 *   any affected creature whose projected power exceeds the source's projected power.
 * - [Duration.WhileSourceOnBattlefield]: ends when the source leaves the battlefield.
 * - [Duration.WhileSourceAttachedToAffected]: keeps only affected objects the source Aura/Equipment
 *   is still attached to — it leaving, detaching, or moving to another host all end it.
 * - [Duration.WhileAffectedTapped]: drops any affected object that is no longer tapped.
 * - [Duration.WhileAffectedHasCounter]: drops any affected object that no longer carries the counter.
 * - [Duration.WhileControlledByController]: drops any affected object the effect's controller no
 *   longer controls.
 * - [Duration.WhileYouControlSource]: ends when the source leaves the battlefield OR its
 *   projected controller is no longer the effect's controller. Drops the entire effect (not
 *   per-affected) — the source-controller half is binary, the source either is or isn't yours.
 *
 * The gates that depend only on the source or the affected object are additionally enforced for
 * duration-keyed grants in `grantedActivatedAbilities` / `grantedStaticAbilities`, which have no
 * floating-effect representation and so would otherwise never expire — Kitesail Larcenist's
 * granted Treasure mana ability is the source-keyed case, Ultima's counter-keyed "{T}: Add {C}"
 * and Braided Net's activation lock the affected-keyed ones.
 *
 * Affected entities no longer on the battlefield are left untouched: the effect as a whole is
 * reaped by the untap-step cleanup / zone-change handling, and we must not emit spurious
 * control-change events for permanents that merely left.
 *
 * Ordered ([SbaOrder.DURATION_EXPIRY]) before [ControlChangedRemovesFromCombatCheck] and the
 * lethal-damage / zero-toughness checks so that, within the same SBA pass, combat removal sees
 * the reverted controller and the death checks see any toughness boost that has now ended.
 */
class EndedDurationExpiryCheck : StateBasedActionCheck {
    override val name = "611.2b Ended-Duration Effect Expiry"
    override val order = SbaOrder.DURATION_EXPIRY

    override fun check(state: GameState): ExecutionResult {
        // A conditional grant (Ultima's counter-keyed "{T}: Add {C}" and Kitesail Larcenist's
        // source-keyed Treasure mana ability in grantedActivatedAbilities; Braided Net's
        // tapped-keyed activation lock in grantedStaticAbilities) must be pruned the moment its
        // condition fails — otherwise a de-blighted land keeps the mana ability / an untapped
        // permanent stays locked / a Treasured permanent keeps saccing for mana after its
        // Larcenist died. This latch is one-way by nature: the grant is never re-added.
        val prunedState = pruneEndedGrants(state)

        if (prunedState.floatingEffects.isEmpty()) {
            return if (prunedState === state) ExecutionResult.success(state)
            else ExecutionResult.success(prunedState)
        }

        val events = mutableListOf<GameEvent>()
        var current = prunedState

        // Internal fixpoint: pruning one effect can change the projected power/controller that
        // gates another, so re-evaluate until the floating-effect set stops shrinking. Each pass
        // only removes entities/effects, so this converges in at most one pass per effect.
        while (true) {
            val projected = current.projectedState
            val pruned = ArrayList<ActiveFloatingEffect>(current.floatingEffects.size)
            var changed = false

            for (floating in current.floatingEffects) {
                val active = activeAffectedEntities(current, projected, floating)
                if (active.size == floating.effect.affectedEntities.size) {
                    pruned.add(floating)
                    continue
                }
                changed = true
                emitControlReversions(current, floating, floating.effect.affectedEntities - active, events)
                if (active.isNotEmpty()) {
                    pruned.add(floating.copy(effect = floating.effect.copy(affectedEntities = active)))
                }
            }

            if (!changed) break
            current = current.copy(floatingEffects = pruned)
        }

        return if (current === state) ExecutionResult.success(state)
        else ExecutionResult.success(current, events)
    }

    /**
     * The subset of [ActiveFloatingEffect.effect]'s affected entities for which the effect's
     * "for as long as" condition still holds. Returns the full set unchanged for non-conditional
     * durations. The returned set is always a subset of the input (the affected set only shrinks),
     * which is what makes the latch one-way.
     */
    private fun activeAffectedEntities(
        state: GameState,
        projected: ProjectedState,
        floating: ActiveFloatingEffect
    ): Set<EntityId> {
        val all = floating.effect.affectedEntities
        return when (floating.duration) {
            // Source-keyed gates — the condition is a pure function of the source permanent (and,
            // for the attachment gate, of which permanent it is attached to). Shared with the
            // granted-ability path via [sourceGateHolds]; filtering per affected entity is
            // equivalent to all-or-nothing for the two gates that ignore the affected object.
            is Duration.WhileSourceTapped,
            is Duration.WhileSourceOnBattlefield,
            Duration.WhileSourceAttachedToAffected ->
                all.filterTo(LinkedHashSet()) {
                    sourceGateHolds(state, floating.duration, floating.sourceId, it)
                }

            is Duration.WhileSourceTappedAndAffectedPowerAtMostSource -> {
                if (!sourceTapped(state, floating.sourceId)) {
                    emptySet()
                } else {
                    val sourcePower = projected.getPower(floating.sourceId!!) ?: return all
                    all.filterTo(LinkedHashSet()) { id ->
                        !state.getBattlefield().contains(id) ||
                            (projected.getPower(id) ?: 0) <= sourcePower
                    }
                }
            }

            Duration.WhileControlledByController ->
                all.filterTo(LinkedHashSet()) { id ->
                    !state.getBattlefield().contains(id) || projected.getController(id) == floating.controllerId
                }

            is Duration.WhileYouControlSource -> {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId)) emptySet()
                else if (projected.getController(sourceId) != floating.controllerId) emptySet()
                else all
            }

            is Duration.WhileAffectedHasCounter -> {
                // "for as long as it has a [X] counter on it" (Ultima) — keep only affected
                // entities that still carry the counter (CR 611.2b). Entities that merely left
                // the battlefield are kept here and reaped by zone-change cleanup instead, matching
                // the other per-affected gates.
                val counterType = CounterType.fromName(floating.duration.counterType) ?: return emptySet()
                all.filterTo(LinkedHashSet()) { id ->
                    !state.getBattlefield().contains(id) ||
                        (state.getEntity(id)?.get<CountersComponent>()?.getCount(counterType) ?: 0) > 0
                }
            }

            Duration.WhileAffectedTapped -> {
                // "for as long as it remains tapped" (Braided Net) — keep only affected entities
                // that are still tapped (CR 611.2b). One-way: once dropped here, a later re-tap
                // does not resurrect the effect. Entities that merely left the battlefield are
                // kept and reaped by zone-change cleanup, matching the other per-affected gates.
                all.filterTo(LinkedHashSet()) { id ->
                    !state.getBattlefield().contains(id) ||
                        state.getEntity(id)?.has<TappedComponent>() == true
                }
            }

            else -> all
        }
    }

    /**
     * Drop any grant whose "for as long as …" condition no longer holds. Two families:
     *
     *  - **Affected-object-keyed** (or whose entity has left the battlefield — a returning
     *    permanent is a new object): [Duration.WhileAffectedHasCounter], the entity no longer
     *    carries the required counter (Ultima's granted "{T}: Add {C}");
     *    [Duration.WhileAffectedTapped], the entity is no longer tapped (Braided Net's granted
     *    activation lock).
     *  - **Source-keyed** ([sourceGateHolds]): the granting permanent left the battlefield,
     *    untapped, or stopped being attached. Kitesail Larcenist's "for as long as this creature
     *    remains on the battlefield" Treasure mana ability lives here — the floating type/ability
     *    effects revert through the per-effect loop in [check], but the granted ability has no
     *    floating-effect representation, so without this it would outlive Kitesail.
     *
     * Covers [GameState.grantedActivatedAbilities] and [GameState.grantedStaticAbilities] — the two
     * grant stores a "for as long as …" duration can reach today. [GameState.grantedTriggeredAbilities],
     * [GameState.grantedReplacementEffects] and [GameState.globalGrantedTriggeredAbilities] are
     * deliberately *not* pruned here: no card grants into them with a conditional duration, so they
     * only ever need the `EndOfTurn` / `UntilYourNextTurn` filters in `CleanupPhaseManager`. Their
     * executors do accept any [Duration] though, so the first card that grants a trigger or
     * replacement effect "for as long as …" has to be added here (and carry a `sourceId` for a
     * source-keyed gate) or it will leak exactly the way the activated-ability grant did.
     *
     * The latch is one-way by nature: a pruned grant is never re-added. Returns the same
     * instance when nothing changed.
     */
    private fun pruneEndedGrants(state: GameState): GameState {
        var result = state
        if (state.grantedActivatedAbilities.any { grantConditionFails(state, it.entityId, it.sourceId, it.duration) }) {
            result = result.copy(
                grantedActivatedAbilities = state.grantedActivatedAbilities.filterNot {
                    grantConditionFails(state, it.entityId, it.sourceId, it.duration)
                }
            )
        }
        if (state.grantedStaticAbilities.any { grantConditionFails(state, it.entityId, it.sourceId, it.duration) }) {
            result = result.copy(
                grantedStaticAbilities = state.grantedStaticAbilities.filterNot {
                    grantConditionFails(state, it.entityId, it.sourceId, it.duration)
                }
            )
        }
        return result
    }

    /**
     * True when [duration] is a conditional "for as long as …" duration whose condition no longer
     * holds for the grant on [entityId] made by [sourceId]. False for every other duration
     * (unconditional grants are never pruned here).
     *
     * The projection-dependent gates ([Duration.WhileSourceTappedAndAffectedPowerAtMostSource]) and
     * the ones that need the *effect's* controller ([Duration.WhileControlledByController],
     * [Duration.WhileYouControlSource]) are deliberately not handled: a grant record carries no
     * controller, and both shapes only ever appear on floating control/pump effects, which the
     * per-effect loop above already reaps.
     */
    private fun grantConditionFails(
        state: GameState,
        entityId: EntityId,
        sourceId: EntityId?,
        duration: Duration
    ): Boolean =
        !sourceGateHolds(state, duration, sourceId, entityId) ||
            affectedGrantConditionFails(state, entityId, duration)

    /**
     * Whether a *source-keyed* "for as long as …" gate still holds for the grant/effect made by
     * [sourceId] on [affectedId]. Returns `true` for every other duration, so callers can apply it
     * unconditionally.
     *
     * Depends only on the source's zone, tapped state, and attachment — no projection — which is
     * what lets the granted-ability path (which has no [ProjectedState] at hand) share it with
     * [activeAffectedEntities]. A missing [sourceId] means there is no source on the battlefield,
     * so the gate is closed.
     */
    private fun sourceGateHolds(
        state: GameState,
        duration: Duration,
        sourceId: EntityId?,
        affectedId: EntityId
    ): Boolean = when (duration) {
        // "for as long as this permanent remains on the battlefield" (Kitesail Larcenist).
        is Duration.WhileSourceOnBattlefield ->
            sourceId != null && state.getBattlefield().contains(sourceId)

        // "for as long as this creature remains tapped" (Old Man of the Sea).
        is Duration.WhileSourceTapped -> sourceTapped(state, sourceId)

        // "for as long as [the source Aura/Equipment] remains attached to it" — the source leaving
        // the battlefield, becoming unattached, or moving to a different host all end it.
        Duration.WhileSourceAttachedToAffected ->
            sourceId != null && state.getBattlefield().contains(sourceId) &&
                state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId == affectedId

        else -> true
    }

    /**
     * True when [duration] is an affected-object-keyed "for as long as …" duration whose
     * condition no longer holds for [entityId]. False for every other duration.
     */
    private fun affectedGrantConditionFails(
        state: GameState,
        entityId: EntityId,
        duration: Duration
    ): Boolean = when (duration) {
        is Duration.WhileAffectedHasCounter -> {
            if (!state.getBattlefield().contains(entityId)) true
            else {
                val counterType = CounterType.fromName(duration.counterType)
                counterType == null ||
                    (state.getEntity(entityId)?.get<CountersComponent>()?.getCount(counterType) ?: 0) <= 0
            }
        }
        Duration.WhileAffectedTapped ->
            !state.getBattlefield().contains(entityId) ||
                state.getEntity(entityId)?.has<TappedComponent>() != true
        else -> false
    }

    private fun sourceTapped(state: GameState, sourceId: EntityId?): Boolean =
        sourceId != null && state.getBattlefield().contains(sourceId) &&
            state.getEntity(sourceId)?.has<TappedComponent>() == true

    /**
     * Emit a [ControlChangedEvent] for each dropped entity of a control effect, reverting from the
     * thief ([SerializableModification.ChangeController.newControllerId]) back to the permanent's
     * base controller. Only control effects produce a visible control change worth an event.
     */
    private fun emitControlReversions(
        state: GameState,
        floating: ActiveFloatingEffect,
        dropped: Set<EntityId>,
        events: MutableList<GameEvent>
    ) {
        val modification = floating.effect.modification
        if (modification !is SerializableModification.ChangeController) return
        for (id in dropped) {
            val container = state.getEntity(id) ?: continue
            val baseController = container.get<ControllerComponent>()?.playerId ?: continue
            if (baseController == modification.newControllerId) continue
            events.add(
                ControlChangedEvent(
                    permanentId = id,
                    permanentName = container.get<CardComponent>()?.name ?: "",
                    oldControllerId = modification.newControllerId,
                    newControllerId = baseController
                )
            )
        }
    }
}
