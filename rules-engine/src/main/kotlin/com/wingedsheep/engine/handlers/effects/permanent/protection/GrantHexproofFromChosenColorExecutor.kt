package com.wingedsheep.engine.handlers.effects.permanent.protection

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
import com.wingedsheep.sdk.scripting.effects.GrantHexproofFromChosenColorEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantHexproofFromChosenColorEffect] — reads `chosenColor` from
 * the effect context (set by the surrounding `ChooseColorThen` resumer) and
 * grants `HEXPROOF_FROM_<COLOR>` to the target.
 */
class GrantHexproofFromChosenColorExecutor : EffectExecutor<GrantHexproofFromChosenColorEffect> {

    override val effectType: KClass<GrantHexproofFromChosenColorEffect> =
        GrantHexproofFromChosenColorEffect::class

    override fun execute(
        state: GameState,
        effect: GrantHexproofFromChosenColorEffect,
        context: EffectContext
    ): EffectResult {
        val color = context.chosenColor
            ?: return EffectResult.error(state, "GrantHexproofFromChosenColor requires a chosen color in context")

        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for hexproof grant")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword("HEXPROOF_FROM_${color.name}"),
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
                keyword = "Hexproof from ${color.displayName.lowercase()}",
                sourceName = sourceName
            )
        )

        return EffectResult.success(newState, events)
    }
}
