package com.wingedsheep.engine.handlers.effects.permanent.stats

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.SwitchPowerToughnessEffect
import kotlin.reflect.KClass

class SwitchPowerToughnessExecutor : EffectExecutor<SwitchPowerToughnessEffect> {
    override val effectType: KClass<SwitchPowerToughnessEffect> = SwitchPowerToughnessEffect::class

    override fun execute(state: GameState, effect: SwitchPowerToughnessEffect, context: EffectContext): EffectResult {
        if (context.isUnavailableBattlefieldSource(effect.target, state)) return EffectResult.success(state)
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)
        val projected = state.projectedState
        val newState = state.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            sublayer = Sublayer.SWITCH,
            modification = SerializableModification.SwitchPowerToughness,
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )
        val power = projected.getPower(targetId) ?: 0
        val toughness = projected.getToughness(targetId) ?: 0
        return EffectResult.success(newState, listOf(StatsModifiedEvent(
            targetId = targetId,
            targetName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Unknown",
            powerChange = toughness - power,
            toughnessChange = power - toughness,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        )))
    }
}
