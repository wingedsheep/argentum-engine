package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for AddCountersEffect.
 * "Put X +1/+1 counters on target creature"
 */
class AddCountersExecutor : EffectExecutor<AddCountersEffect> {

    override val effectType: KClass<AddCountersEffect> = AddCountersEffect::class

    override fun execute(
        state: GameState,
        effect: AddCountersEffect,
        context: EffectContext
    ): EffectResult {
        // Counters usually go on a permanent, but "that player gets two poison counters"
        // (Virulent Silencer) puts them on a player — a PlayerRef target only resolves through
        // the player-resolution path, so try it when the target denotes a player.
        val targetId = if (effect.target is EffectTarget.PlayerRef) {
            context.resolvePlayerTarget(effect.target, state)
        } else {
            context.resolveTarget(effect.target, state)
        } ?: return EffectResult.error(state, "No valid target for counters")

        if (!state.projectedState.canReceiveCounters(targetId)) {
            return EffectResult.success(state, emptyList())
        }

        val counterType = resolveCounterType(effect.counterType)

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        // Apply counter placement replacement effects (e.g., Hardened Scales)
        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, targetId, counterType, effect.count, placerId = context.controllerId
        )

        val firstThisTurn = DamageUtils.isFirstCounterThisTurn(state, targetId)

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withAdded(counterType, modifiedCount))
        }.let { DamageUtils.markCounterPlacedOnCreature(it, context.controllerId, targetId, counterTypeToString(counterType)) }

        val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""

        return EffectResult.success(
            newState,
            listOf(CountersAddedEvent(targetId, effect.counterType, modifiedCount, entityName, firstThisTurn, placedBy = context.controllerId))
        )
    }
}
