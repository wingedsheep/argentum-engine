package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PlayerActionPermissionsChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.BlockerCountLimitThisCombatComponent
import com.wingedsheep.sdk.scripting.effects.BlockerCountLimitThisCombatEffect
import kotlin.reflect.KClass

/**
 * Executor for [BlockerCountLimitThisCombatEffect]: stamps [BlockerCountLimitThisCombatComponent]
 * on each target player, keeping the smaller cap when one is already there. `BlockPhaseManager`
 * enforces it when that player declares blockers; `CombatManager.endCombat` removes it.
 */
class BlockerCountLimitThisCombatExecutor : EffectExecutor<BlockerCountLimitThisCombatEffect> {

    override val effectType: KClass<BlockerCountLimitThisCombatEffect> = BlockerCountLimitThisCombatEffect::class

    override fun execute(
        state: GameState,
        effect: BlockerCountLimitThisCombatEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.resolvePlayerTargets(effect.target, state)
            .filter { it in state.turnOrder }
        if (targetIds.isEmpty()) return EffectResult.success(state)

        val newState = targetIds.fold(state) { acc, playerId ->
            acc.updateEntity(playerId) { container ->
                val existing = container.get<BlockerCountLimitThisCombatComponent>()?.maxBlockers
                val cap = if (existing == null) effect.maxBlockers else minOf(existing, effect.maxBlockers)
                container.with(BlockerCountLimitThisCombatComponent(cap))
            }
        }
        return EffectResult.success(newState, targetIds.map(::PlayerActionPermissionsChangedEvent))
    }
}
