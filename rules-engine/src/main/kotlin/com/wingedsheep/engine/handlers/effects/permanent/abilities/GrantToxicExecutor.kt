package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.effects.GrantToxicEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantToxicEffect] — grants Toxic N to a target via a
 * `TOXIC_<n>` keyword floating effect. Combat damage reads granted toxic
 * amounts from projected keywords.
 */
class GrantToxicExecutor : EffectExecutor<GrantToxicEffect> {

    override val effectType: KClass<GrantToxicEffect> = GrantToxicEffect::class

    override fun execute(
        state: GameState,
        effect: GrantToxicEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for toxic grant")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        if (!state.projectedState.isCreature(targetId)) {
            return EffectResult.error(state, "Target is not a creature")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword("TOXIC_${effect.amount}"),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        val displayName = if (container.has<FaceDownComponent>()) "Face-down creature" else cardComponent.name
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = displayName,
                keyword = "Toxic ${effect.amount}",
                sourceName = sourceName
            )
        )

        return EffectResult.success(newState, events)
    }
}
