package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.BecameCrewedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BecomeCrewedEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeCrewedEffect] — the last step of a crew ability's resolution. The Vehicle
 * "becomes crewed" whenever a crew ability of it resolves (CR 702.122e): count the resolution on
 * its [CrewSaddleContributorsComponent] and emit a [BecameCrewedEvent].
 */
class BecomeCrewedExecutor : EffectExecutor<BecomeCrewedEffect> {

    override val effectType: KClass<BecomeCrewedEffect> = BecomeCrewedEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeCrewedEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)
        // A Vehicle that left the battlefield before its crew ability resolved isn't crewed.
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)

        val name = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Unknown"
        val existing = state.getEntity(targetId)?.get<CrewSaddleContributorsComponent>()
            ?: CrewSaddleContributorsComponent(creatureIds = emptySet())
        val firstThisTurn = existing.crewResolutions == 0
        val newState = state.updateEntity(targetId) {
            it.with(existing.copy(crewResolutions = existing.crewResolutions + 1))
        }
        return EffectResult.success(newState, listOf(BecameCrewedEvent(targetId, name, firstThisTurn)))
    }
}
