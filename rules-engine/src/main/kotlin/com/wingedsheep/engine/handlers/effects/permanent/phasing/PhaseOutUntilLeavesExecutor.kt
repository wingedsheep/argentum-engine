package com.wingedsheep.engine.handlers.effects.permanent.phasing

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PhasedOutEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PhaseOutUntilLeavesEffect
import kotlin.reflect.KClass

/**
 * Executor for [PhaseOutUntilLeavesEffect] — the phasing analogue of `ExileUntilLeavesEffect`
 * (Oubliette). Phases the target permanent out (with indirect phasing of its attachments,
 * Rule 702.26g) and links each phased-out permanent to the effect's source via
 * [PhasedOutComponent.phaseInOnSourceLeaves], so the untap step leaves them out and the source's
 * leaves-battlefield trigger ([PhaseInLinkedToSourceExecutor]) phases them back in.
 *
 * The targeted creature itself carries [PhasedOutComponent.tapOnPhaseIn] (when requested) so it
 * phases in tapped — its attachments do not tap.
 */
class PhaseOutUntilLeavesExecutor : EffectExecutor<PhaseOutUntilLeavesEffect> {

    override val effectType: KClass<PhaseOutUntilLeavesEffect> = PhaseOutUntilLeavesEffect::class

    override fun execute(
        state: GameState,
        effect: PhaseOutUntilLeavesEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        // Modern template: if the source already left the battlefield, do nothing.
        if (sourceId !in state.getBattlefield()) return EffectResult.success(state)

        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)

        val controllerId = state.projectedState.getController(targetId) ?: context.controllerId
        val toPhaseOut = collectWithAttachments(state, targetId)

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (entityId in toPhaseOut) {
            if (newState.getEntity(entityId)?.has<PhasedOutComponent>() == true) continue
            val name = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) {
                it.with(
                    PhasedOutComponent(
                        phasedOutByController = controllerId,
                        phaseInOnSourceLeaves = sourceId,
                        // Only the targeted permanent taps as it phases in; attachments don't.
                        tapOnPhaseIn = effect.tapOnPhaseIn && entityId == targetId
                    )
                )
            }
            events.add(PhasedOutEvent(entityId, name))
        }

        return EffectResult.success(newState, events)
    }

    /** The entity plus the transitive set of permanents attached to it (indirect phasing). */
    private fun collectWithAttachments(state: GameState, rootId: EntityId): List<EntityId> {
        val result = mutableListOf<EntityId>()
        val seen = mutableSetOf<EntityId>()
        val queue = ArrayDeque<EntityId>()
        queue.add(rootId)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!seen.add(id)) continue
            result.add(id)
            val attachments = state.getEntity(id)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
            queue.addAll(attachments)
        }
        return result
    }
}
