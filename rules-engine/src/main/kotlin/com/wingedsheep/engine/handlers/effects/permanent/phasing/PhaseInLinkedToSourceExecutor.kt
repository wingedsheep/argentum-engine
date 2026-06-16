package com.wingedsheep.engine.handlers.effects.permanent.phasing

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PhasedInEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.PhaseInLinkedToSourceEffect
import kotlin.reflect.KClass

/**
 * Executor for [PhaseInLinkedToSourceEffect] — used on the source's leaves-battlefield trigger
 * (Oubliette). Phases in every permanent that was phased out "until this source leaves" (matched on
 * [PhasedOutComponent.phaseInOnSourceLeaves] == the trigger's source), removing its phased-out mark
 * and tapping it if it was flagged [PhasedOutComponent.tapOnPhaseIn].
 *
 * The source has already left the battlefield by the time this resolves; `context.sourceId` still
 * identifies it (leaves-battlefield triggers carry their source), so the link match works.
 */
class PhaseInLinkedToSourceExecutor : EffectExecutor<PhaseInLinkedToSourceEffect> {

    override val effectType: KClass<PhaseInLinkedToSourceEffect> = PhaseInLinkedToSourceEffect::class

    override fun execute(
        state: GameState,
        effect: PhaseInLinkedToSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)

        val toPhaseIn = state.allBattlefieldEntities().filter { entityId ->
            state.getEntity(entityId)?.get<PhasedOutComponent>()?.phaseInOnSourceLeaves == sourceId
        }
        if (toPhaseIn.isEmpty()) return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (entityId in toPhaseIn) {
            val phased = newState.getEntity(entityId)?.get<PhasedOutComponent>() ?: continue
            val name = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { container ->
                var updated = container.without<PhasedOutComponent>()
                if (phased.tapOnPhaseIn) updated = updated.with(TappedComponent)
                updated
            }
            events.add(PhasedInEvent(entityId, name))
        }

        return EffectResult.success(newState, events)
    }
}
