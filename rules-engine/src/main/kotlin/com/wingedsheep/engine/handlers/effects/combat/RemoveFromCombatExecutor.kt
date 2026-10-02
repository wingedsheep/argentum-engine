package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.RemovedFromCombatEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveFromCombatEffect ("Remove [target] from combat.").
 *
 * Cleanup is delegated to [CombatRemovalHelper] so the CR 506.4 state-based check shares
 * the same logic.
 */
class RemoveFromCombatExecutor : EffectExecutor<RemoveFromCombatEffect> {

    override val effectType: KClass<RemoveFromCombatEffect> = RemoveFromCombatEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveFromCombatEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)
        val updated = CombatRemovalHelper.removeFromCombat(
            state,
            targetId,
            unblockSoleBlockedAttackers = effect.unblockSoleBlockedAttackers,
        )
        return EffectResult.success(updated, if (updated == state) emptyList() else listOf(RemovedFromCombatEvent(targetId)))
    }
}
