package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.chain.ChainExecutors
import com.wingedsheep.engine.handlers.effects.combat.CombatExecutors
import com.wingedsheep.engine.handlers.effects.composite.CompositeExecutors
import com.wingedsheep.engine.handlers.effects.damage.DamageExecutors
import com.wingedsheep.engine.handlers.effects.drawing.DrawingExecutors
import com.wingedsheep.engine.handlers.effects.information.InformationExecutors
import com.wingedsheep.engine.handlers.effects.library.LibraryExecutors
import com.wingedsheep.engine.handlers.effects.life.LifeExecutors
import com.wingedsheep.engine.handlers.effects.mana.ManaExecutors
import com.wingedsheep.engine.handlers.effects.permanent.PermanentExecutors
import com.wingedsheep.engine.handlers.effects.player.PlayerExecutors
import com.wingedsheep.engine.handlers.effects.removal.RemovalExecutors
import com.wingedsheep.engine.handlers.effects.stack.StackExecutors
import com.wingedsheep.engine.handlers.effects.token.TokenExecutors
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Registry that maps effect types to their executors.
 *
 * This implements the Strategy pattern, allowing each effect type to have
 * its own dedicated executor class while providing a unified dispatch mechanism.
 *
 * The registry uses a map-based dispatch system with modular sub-registries
 * for each category of effects, reducing merge conflicts and enabling
 * dynamic executor registration.
 */
class EffectExecutorRegistry(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry? = null
) {
    private val executors = mutableMapOf<KClass<out Effect>, EffectExecutor<*>>()
    private val compositeExecutors = CompositeExecutors()
    private val drawingExecutors = DrawingExecutors(amountEvaluator, decisionHandler, cardRegistry = cardRegistry)
    private val removalExecutors = RemovalExecutors(cardRegistry)

    init {
        // Register all effect executors by module
        registerModule(LifeExecutors(amountEvaluator))
        registerModule(DamageExecutors(amountEvaluator, decisionHandler))
        registerModule(PermanentExecutors(decisionHandler, amountEvaluator))
        registerModule(ManaExecutors(amountEvaluator))
        registerModule(TokenExecutors(amountEvaluator))
        registerModule(LibraryExecutors(cardRegistry = cardRegistry, targetFinder = TargetFinder()))
        registerModule(StackExecutors(amountEvaluator))
        registerModule(PlayerExecutors(decisionHandler))
        registerModule(InformationExecutors())
        registerModule(CombatExecutors(amountEvaluator))
        registerModule(ChainExecutors())

        // Deferred initialization for recursive executors
        compositeExecutors.initialize(::execute)
        registerModule(compositeExecutors)
        drawingExecutors.initialize(::execute)
        registerModule(drawingExecutors)
        removalExecutors.initialize(::execute)
        registerModule(removalExecutors)
    }

    /**
     * Register all executors from a module.
     */
    fun registerModule(module: ExecutorModule) {
        module.executors().forEach { executor ->
            executors[executor.effectType] = executor
        }
    }

    /**
     * Register a single executor.
     * Useful for dynamic registration at runtime.
     */
    fun <T : Effect> register(executor: EffectExecutor<T>) {
        executors[executor.effectType] = executor
    }

    /**
     * Execute an effect using the appropriate executor.
     *
     * @param state The current game state
     * @param effect The effect to execute
     * @param context The execution context
     * @return The execution result with new state and events
     */
    @Suppress("UNCHECKED_CAST")
    fun execute(state: GameState, effect: Effect, context: EffectContext): ExecutionResult {
        val executor = executors[effect::class] as? EffectExecutor<Effect>
            ?: return ExecutionResult.success(state) // Unhandled effect type
        return executor.execute(state, effect, context)
    }

    /**
     * Returns the number of registered executors.
     * Useful for testing and diagnostics.
     */
    fun executorCount(): Int = executors.size
}
