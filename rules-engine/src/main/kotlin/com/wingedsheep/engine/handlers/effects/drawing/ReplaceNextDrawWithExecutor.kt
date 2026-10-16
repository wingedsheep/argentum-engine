package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ReplaceNextDrawWithEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReplaceNextDrawWithEffect].
 *
 * Creates a floating replacement-effect shield that intercepts [ReplaceNextDrawWithEffect.player]'s
 * (by default the controller's) next card draw this turn, replacing it with the stored [ReplaceNextDrawWithEffect.replacementEffect].
 *
 * The replacement effect is stored directly in the shield as a generic [SerializableModification.ReplaceDrawWith],
 * which is consumed at draw time by delegating to the effect execution pipeline.
 */
class ReplaceNextDrawWithExecutor : EffectExecutor<ReplaceNextDrawWithEffect> {

    override val effectType: KClass<ReplaceNextDrawWithEffect> = ReplaceNextDrawWithEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithEffect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val drawingPlayerId = context.resolvePlayerTarget(effect.player, state)
            ?: return EffectResult.success(state)

        val modification = SerializableModification.ReplaceDrawWith(
            replacementEffect = effect.replacementEffect,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            sourceId = context.sourceId,
            sourceName = sourceName,
            // Capture the activation-time {X} so the replacement effect can read it at draw time.
            xValue = context.xValue,
            drawingPlayerId = drawingPlayerId
        )

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = modification,
            affectedEntities = setOf(drawingPlayerId),
            duration = Duration.NextUse(
                if (drawingPlayerId == context.controllerId) "you would draw a card" else "that player would draw a card"
            ),
            context = context
        )

        return EffectResult.success(newState)
    }
}
