package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.EffectResult
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
import com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileExecutors
import com.wingedsheep.engine.handlers.effects.permanent.PermanentExecutors
import com.wingedsheep.engine.handlers.effects.player.PlayerExecutors
import com.wingedsheep.engine.handlers.effects.regeneration.RegenerationExecutors
import com.wingedsheep.engine.handlers.effects.stack.StackExecutors
import com.wingedsheep.engine.handlers.effects.token.TokenExecutors
import com.wingedsheep.engine.handlers.effects.zones.ZonesExecutors
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
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
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry
) {
    private val executors = mutableMapOf<KClass<out Effect>, EffectExecutor<*>>()
    private val compositeExecutors = CompositeExecutors(cardRegistry, TargetFinder(), decisionHandler)
    private val drawingExecutors = DrawingExecutors(amountEvaluator, decisionHandler, cardRegistry = cardRegistry)
    private val playerExecutors = PlayerExecutors(decisionHandler, cardRegistry)
    private val chainExecutors = ChainExecutors()

    /**
     * Exposed so [com.wingedsheep.engine.core.EngineServices] can call
     * [LibraryExecutors.initialize] once the rest of the service graph is wired.
     */
    val libraryExecutors: LibraryExecutors = LibraryExecutors(cardRegistry = cardRegistry, targetFinder = TargetFinder())

    init {
        // Register all effect executors by module
        registerModule(LifeExecutors(amountEvaluator))
        registerModule(DamageExecutors(amountEvaluator, decisionHandler))
        registerModule(PermanentExecutors(decisionHandler, amountEvaluator, cardRegistry))
        registerModule(ManaExecutors(amountEvaluator, cardRegistry))
        registerModule(TokenExecutors(amountEvaluator, StaticAbilityHandler(cardRegistry), cardRegistry))
        // The scry/surveil macro executors expand to a composite pipeline and delegate back through
        // [recurse]; wire it in before registering (the ref is read lazily, so order is not load-bearing).
        libraryExecutors.initializeRecursion(::recurse)
        registerModule(libraryExecutors)
        registerModule(StackExecutors(amountEvaluator, cardRegistry))
        registerModule(InformationExecutors())
        registerModule(CombatExecutors(amountEvaluator))
        registerModule(ZonesExecutors(cardRegistry))
        registerModule(LinkedExileExecutors())
        registerModule(RegenerationExecutors())

        // Deferred initialization for recursive executors. They recurse through [recurse], which
        // deepens [EffectContext.resolutionDepth] by one per nested sub-effect so [execute] can cap
        // runaway recursion (see GameLimits.MAX_RESOLUTION_DEPTH).
        compositeExecutors.initialize(::recurse)
        registerModule(compositeExecutors)
        drawingExecutors.initialize(::recurse)
        registerModule(drawingExecutors)
        playerExecutors.initialize(::recurse)
        registerModule(playerExecutors)
        chainExecutors.initialize(::recurse)
        registerModule(chainExecutors)
    }

    /**
     * Recursion entry point handed to composite/iteration executors. Deepens the resolution depth
     * carried on the (immutable) [EffectContext] so the [execute] guard sees nesting/iteration
     * grow. Using the context — not a mutable field on this shared registry — keeps the count
     * correct under the AI's parallel state evaluation.
     */
    private fun recurse(state: GameState, effect: Effect, context: EffectContext): EffectResult =
        execute(state, effect, context.copy(resolutionDepth = context.resolutionDepth + 1))

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
    fun execute(state: GameState, effect: Effect, context: EffectContext): EffectResult {
        // Resolution-depth backstop: a self-perpetuating effect loop (e.g. a RepeatWhileEffect whose
        // condition never goes false) recurses through [recurse], deepening resolutionDepth each
        // time. Bail before the JVM call stack does. Returning an error fizzles this branch of the
        // resolution rather than crashing the game — the correct outcome for a degenerate loop.
        if (context.resolutionDepth > com.wingedsheep.engine.core.GameLimits.MAX_RESOLUTION_DEPTH) {
            System.err.println(
                "EffectExecutorRegistry: resolution depth exceeded " +
                    "${com.wingedsheep.engine.core.GameLimits.MAX_RESOLUTION_DEPTH} executing " +
                    "${effect::class.simpleName} — aborting this effect branch (likely an unbounded " +
                    "effect loop)."
            )
            return EffectResult.error(
                state,
                "Effect resolution depth exceeded; aborting to avoid stack overflow"
            )
        }
        val executor = executors[effect::class] as? EffectExecutor<Effect>
            ?: error(
                "No executor registered for effect type ${effect::class.simpleName}. " +
                    "Register one in the matching *Executors module " +
                    "(EffectExecutorCoverageTest guards this at build time)."
            )
        return executor.execute(state, effect, context)
    }

    /**
     * Returns the number of registered executors.
     * Useful for testing and diagnostics.
     */
    fun executorCount(): Int = executors.size

    /**
     * Returns the effect types that have a registered executor.
     * Used by the executor-coverage hygiene test to verify every concrete [Effect]
     * subtype is either executable or declared as a non-executable marker.
     */
    fun registeredEffectTypes(): Set<KClass<out Effect>> = executors.keys.toSet()
}
