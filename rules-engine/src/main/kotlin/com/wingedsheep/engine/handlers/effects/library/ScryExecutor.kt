package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.ScryEffect
import kotlin.reflect.KClass

/**
 * Executor for ScryEffect.
 * "Scry X" - Look at the top X cards of your library, then put any number
 * on the bottom of your library and the rest on top in any order.
 */
class ScryExecutor : EffectExecutor<ScryEffect> {

    override val effectType: KClass<ScryEffect> = ScryEffect::class

    override fun execute(
        state: GameState,
        effect: ScryEffect,
        context: EffectContext
    ): ExecutionResult {
        // Scry is a complex effect requiring player choice
        // For now, just tick the state timestamp
        return ExecutionResult.success(state.tick())
    }
}
