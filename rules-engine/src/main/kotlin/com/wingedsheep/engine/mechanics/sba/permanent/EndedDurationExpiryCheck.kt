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
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
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
 * - [Duration.WhileControlledByController]: drops any affected object the effect's controller no
 *   longer controls.
 * - [Duration.WhileYouControlSource]: ends when the source leaves the battlefield OR its
 *   projected controller is no longer the effect's controller. Drops the entire effect (not
 *   per-affected) — the source-controller half is binary, the source either is or isn't yours.
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
        if (state.floatingEffects.isEmpty()) return ExecutionResult.success(state)

        val events = mutableListOf<GameEvent>()
        var current = state

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
            is Duration.WhileSourceTapped ->
                if (sourceTapped(state, floating.sourceId)) all else emptySet()

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

            is Duration.WhileSourceOnBattlefield ->
                if (floating.sourceId != null && state.getBattlefield().contains(floating.sourceId)) all
                else emptySet()

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

            Duration.WhileSourceAttachedToAffected -> {
                // "for as long as [the source Aura/Equipment] remains attached to it" — keep only
                // affected entities the source is still attached to (CR 611.2b). The source leaving
                // the battlefield, becoming unattached, or moving to a different host all drop it.
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId)) emptySet()
                else {
                    val attachedTo = state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                        ?.targetId
                    all.filterTo(LinkedHashSet()) { it == attachedTo }
                }
            }

            else -> all
        }
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
