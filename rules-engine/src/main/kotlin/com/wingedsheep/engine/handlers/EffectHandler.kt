package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Executes effects from the SDK against the game state.
 *
 * This is the bridge between the "dumb" effect data and the "smart" engine logic.
 * Each effect type maps to a specific executor in the registry.
 *
 * This class serves as a facade over the EffectExecutorRegistry, maintaining
 * backward compatibility while delegating execution to individual executors.
 */
class EffectHandler(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    /**
     * Executor registry to delegate to. [com.wingedsheep.engine.core.EngineServices] passes its
     * own, so effects resolving off the stack share the fully-wired registry (in particular its
     * [com.wingedsheep.engine.registry.TokenArtRegistry]) rather than a bare one built here.
     * Left null, this builds its own — the standalone default for callers with no service graph.
     */
    registry: EffectExecutorRegistry? = null,
) {
    private val registry = registry ?: EffectExecutorRegistry(amountEvaluator, cardRegistry = cardRegistry)

    /**
     * Execute an effect and return the result.
     */
    fun execute(
        state: GameState,
        effect: Effect,
        context: EffectContext
    ): EffectResult {
        return registry.execute(state, effect, context)
    }
}
