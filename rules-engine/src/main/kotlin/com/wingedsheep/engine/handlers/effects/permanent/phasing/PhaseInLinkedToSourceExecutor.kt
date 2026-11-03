package com.wingedsheep.engine.handlers.effects.permanent.phasing

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PhasedInEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
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
 *
 * While a [com.wingedsheep.sdk.scripting.CantPhaseIn] static holds nothing phases in: the
 * "until the source leaves" duration has still ended, so its link (and the tap-as-it-phases-in
 * rider that belonged to this phase-in) is cleared, and it phases in at a later untap step of its
 * controller's like any other phased-out permanent once nothing forbids it.
 */
class PhaseInLinkedToSourceExecutor(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<PhaseInLinkedToSourceEffect> {

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

        val locked = PhaseInLocks.locked(state, cardRegistry, predicateEvaluator)
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (entityId in toPhaseIn) {
            val phased = newState.getEntity(entityId)?.get<PhasedOutComponent>() ?: continue
            if (locked) {
                newState = newState.updateEntity(entityId) {
                    it.with(phased.copy(phaseInOnSourceLeaves = null, tapOnPhaseIn = false))
                }
                continue
            }
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
