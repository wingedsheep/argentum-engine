package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedByChosenColorEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantCantBeBlockedByChosenColorEffect] — reads `chosenColor`
 * from the effect context (set by the surrounding `ChooseColorThen` resumer)
 * and adds a `CantBeBlockedByColor` floating effect for the target. The combat
 * `CantBeBlockedByColorRule` consults that floating effect at block-declaration time.
 */
class GrantCantBeBlockedByChosenColorExecutor : EffectExecutor<GrantCantBeBlockedByChosenColorEffect> {

    override val effectType: KClass<GrantCantBeBlockedByChosenColorEffect> =
        GrantCantBeBlockedByChosenColorEffect::class

    override fun execute(
        state: GameState,
        effect: GrantCantBeBlockedByChosenColorEffect,
        context: EffectContext
    ): EffectResult {
        val color = context.chosenColor
            ?: return EffectResult.error(state, "GrantCantBeBlockedByChosenColor requires a chosen color in context")

        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for unblockability grant")

        if (!state.getBattlefield().contains(targetId)) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.CantBeBlockedByColor(color.name),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
