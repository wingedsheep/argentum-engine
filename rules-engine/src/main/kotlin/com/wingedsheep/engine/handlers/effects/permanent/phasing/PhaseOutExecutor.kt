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
import com.wingedsheep.sdk.scripting.effects.PhaseOutEffect
import kotlin.reflect.KClass

/**
 * Executor for [PhaseOutEffect] (Rule 702.26).
 *
 * Marks the target permanent — and, by indirect phasing (Rule 702.26g), everything
 * attached to it — with [PhasedOutComponent]. The phased-out permanents are then
 * hidden by [GameState.getBattlefield], so they stop existing for projection, combat,
 * targeting, triggers, and state-based actions until `BeginningPhaseManager` phases
 * them back in before their controller's next untap step.
 *
 * Phasing is not a zone change, so no leaves-the-battlefield triggers fire and the
 * permanents keep their tapped state, counters, and attachments.
 */
class PhaseOutExecutor : EffectExecutor<PhaseOutEffect> {

    override val effectType: KClass<PhaseOutEffect> = PhaseOutEffect::class

    override fun execute(
        state: GameState,
        effect: PhaseOutEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        // Record who controls the permanent now; while phased out it has no projected
        // controller, and it phases in during this player's next untap step.
        val controllerId = state.projectedState.getController(targetId) ?: context.controllerId

        // Indirect phasing: the permanent and everything attached to it phase out together.
        val toPhaseOut = collectWithAttachments(state, targetId)

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (entityId in toPhaseOut) {
            if (newState.getEntity(entityId)?.has<PhasedOutComponent>() == true) continue
            val name = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.with(PhasedOutComponent(controllerId)) }
            events.add(PhasedOutEvent(entityId, name))
        }

        return EffectResult.success(newState, events)
    }

    /** The entity plus the transitive set of permanents attached to it. */
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
