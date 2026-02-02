package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.CantBeRegeneratedEffect
import kotlin.reflect.KClass

/**
 * Executor for CantBeRegeneratedEffect.
 * Marks target as unable to regenerate.
 *
 * This effect is designed to work AFTER DestroyEffect in a composite,
 * so the target may be in the graveyard. The target resolution must
 * track the entity across zone changes.
 *
 * Currently a no-op since regeneration isn't implemented yet.
 * When regeneration is added, this will mark the entity to prevent
 * regeneration effects from returning it to the battlefield.
 */
class CantBeRegeneratedExecutor : EffectExecutor<CantBeRegeneratedEffect> {

    override val effectType: KClass<CantBeRegeneratedEffect> = CantBeRegeneratedEffect::class

    override fun execute(
        state: GameState,
        effect: CantBeRegeneratedEffect,
        context: EffectContext
    ): ExecutionResult {
        // Note: Target may be in graveyard after DestroyEffect
        // resolveTarget should find the entity regardless of zone
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for can't be regenerated")

        // TODO: When regeneration is implemented:
        // 1. Find entity (may be in graveyard)
        // 2. Mark it as CantBeRegenerated (prevents "return to battlefield" regen effects)

        // For now, successfully resolve but do nothing (regeneration not yet implemented)
        return ExecutionResult.success(state)
    }
}
