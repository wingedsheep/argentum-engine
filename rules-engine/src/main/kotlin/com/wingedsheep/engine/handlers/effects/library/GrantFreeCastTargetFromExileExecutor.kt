package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ExileAfterResolveComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantFreeCastTargetFromExileEffect.
 *
 * Marks a single target entity in exile with MayPlayFromExileComponent and
 * PlayWithoutPayingCostComponent, granting the controller permission to cast
 * it from exile without paying its mana cost. Optionally adds
 * ExileAfterResolveComponent so the spell goes to exile instead of graveyard
 * after resolution.
 */
class GrantFreeCastTargetFromExileExecutor : EffectExecutor<GrantFreeCastTargetFromExileEffect> {

    override val effectType: KClass<GrantFreeCastTargetFromExileEffect> =
        GrantFreeCastTargetFromExileEffect::class

    override fun execute(
        state: GameState,
        effect: GrantFreeCastTargetFromExileEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)

        val newState = state.updateEntity(targetId) { container ->
            var updated = container
                .with(MayPlayFromExileComponent(controllerId = controllerId))
                .with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            if (effect.exileAfterResolve) {
                updated = updated.with(ExileAfterResolveComponent)
            }
            updated
        }

        return EffectResult.success(newState)
    }
}
