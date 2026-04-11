package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import kotlin.reflect.KClass

/**
 * Executor for SacrificeSelfEffect.
 *
 * Sacrifices the source permanent (the card that has this effect).
 */
class SacrificeSelfExecutor : EffectExecutor<SacrificeSelfEffect> {

    override val effectType: KClass<SacrificeSelfEffect> = SacrificeSelfEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeSelfEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state) // No source to sacrifice

        val controllerId = context.controllerId
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)

        // Check if the source is still on the battlefield
        if (sourceId !in state.getZone(battlefieldZone)) {
            return ExecutionResult.success(state)
        }

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        var newState = ZoneTransitionService.trackFoodSacrifice(state, listOf(sourceId), controllerId)

        val transitionResult = ZoneTransitionService.moveToZone(
            newState, sourceId, Zone.GRAVEYARD, fromZoneKey = battlefieldZone
        )

        val events = mutableListOf<GameEvent>()
        events.add(PermanentsSacrificedEvent(controllerId, listOf(sourceId), listOf(sourceName)))
        events.addAll(transitionResult.events)

        return ExecutionResult.success(transitionResult.state, events)
    }
}
