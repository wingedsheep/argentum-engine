package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.AdditionalCombatPhasesComponent
import com.wingedsheep.sdk.scripting.AddCombatPhaseEffect
import kotlin.reflect.KClass

/**
 * Executor for AddCombatPhaseEffect.
 * "After this main phase, there is an additional combat phase followed by
 * an additional main phase."
 *
 * Adds (or increments) an AdditionalCombatPhasesComponent on the active player.
 * The TurnManager checks this component when advancing from POSTCOMBAT_MAIN
 * and redirects to BEGIN_COMBAT instead of END step.
 */
class AddCombatPhaseExecutor : EffectExecutor<AddCombatPhaseEffect> {

    override val effectType: KClass<AddCombatPhaseEffect> = AddCombatPhaseEffect::class

    override fun execute(
        state: GameState,
        effect: AddCombatPhaseEffect,
        context: EffectContext
    ): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player for AddCombatPhaseEffect")

        val existing = state.getEntity(activePlayer)?.get<AdditionalCombatPhasesComponent>()
        val newCount = (existing?.count ?: 0) + 1

        val newState = state.updateEntity(activePlayer) { container ->
            container.with(AdditionalCombatPhasesComponent(count = newCount))
        }

        return ExecutionResult.success(newState)
    }
}
