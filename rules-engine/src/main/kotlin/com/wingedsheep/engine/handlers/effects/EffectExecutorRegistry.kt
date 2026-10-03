package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.actions.land.PlayLandHandler
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.chain.ChainExecutors
import com.wingedsheep.engine.handlers.effects.combat.CombatExecutors
import com.wingedsheep.engine.handlers.effects.bend.BendExecutors
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
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
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
    private val zones: ZoneTransitionService,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry? = null,
    replacementProcessor: com.wingedsheep.engine.replacement.ReplacementEffectProcessor,
    spellCounterer: SpellCounterer,
    /**
     * The engine's cast and land-play pipelines, for the "cast / play it without paying its mana
     * cost" executors, and its cost-payment service, for "pay or suffer". Providers, because those
     * are built from the whole engine graph — this registry included — so
     * [com.wingedsheep.engine.core.EngineServices] builds them last.
     */
    castSpellHandler: () -> CastSpellHandler,
    playLandHandler: () -> PlayLandHandler,
    costPaymentService: () -> com.wingedsheep.engine.mechanics.cost.CostPaymentService,
    private val targetFinder: TargetFinder,
    private val targetValidator: TargetValidator,
    legalActionEnumerator: () -> com.wingedsheep.engine.legalactions.LegalActionEnumerator
) {
    private val predicateEvaluator = zones.predicateEvaluator
    private val executors = mutableMapOf<KClass<out Effect>, EffectExecutor<*>>()
    private val amountEvaluator: DynamicAmountEvaluator = predicateEvaluator.amounts

    init {
        // Every module that runs sub-effects receives [recurse] at construction; the reference is
        // only invoked once an effect executes, by which point the registry is fully built.
        registerModule(LifeExecutors(zones, amountEvaluator, cardRegistry, replacementProcessor, ::recurse))
        registerModule(DamageExecutors(zones, amountEvaluator, decisionHandler))
        registerModule(PermanentExecutors(::recurse, zones, decisionHandler, amountEvaluator, cardRegistry))
        registerModule(ManaExecutors(amountEvaluator, cardRegistry))
        registerModule(TokenExecutors(zones, amountEvaluator, StaticAbilityHandler(cardRegistry), cardRegistry, tokenArtRegistry, targetFinder = targetFinder))
        registerModule(
            LibraryExecutors(::recurse, zones, cardRegistry, castSpellHandler, playLandHandler, targetFinder, legalActionEnumerator)
        )
        registerModule(StackExecutors(zones, amountEvaluator, cardRegistry, spellCounterer, targetFinder = targetFinder))
        registerModule(InformationExecutors())
        registerModule(CombatExecutors(amountEvaluator, cardRegistry))
        registerModule(ZonesExecutors(::recurse, zones, cardRegistry, targetFinder = targetFinder))
        registerModule(LinkedExileExecutors(zones))
        registerModule(RegenerationExecutors())
        registerModule(BendExecutors())
        registerModule(CompositeExecutors(::recurse, cardRegistry, targetFinder, decisionHandler, amountEvaluator = amountEvaluator, targetValidator = targetValidator))
        registerModule(
            DrawingExecutors(
                ::recurse,
                zones,
                amountEvaluator,
                decisionHandler,
                cardRegistry = cardRegistry,
                replacementProcessor = replacementProcessor,
                targetFinder = targetFinder
            )
        )
        registerModule(PlayerExecutors(::recurse, zones, decisionHandler, cardRegistry, costPaymentService))
        registerModule(ChainExecutors(::recurse, targetFinder = targetFinder, predicateEvaluator = predicateEvaluator))
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
        val instructionContext = context.withCurrentObjectReferences(state)
        val executed = executor.execute(state, effect, instructionContext)
        val result = if (effect is com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect ||
            effect is com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect) {
            executed.copy(updatedCollections = EffectDiscardDestinations.propagateUnknown(
                executed.updatedCollections, instructionContext.pipeline.storedCollections,
                when (effect) {
                    is com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect -> effect.from
                    is com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect -> effect.from
                    else -> null
                }))
        } else executed
        val references = instructionContext.objectReferences.authorize(result.events)
        val finished = result.copy(state = com.wingedsheep.engine.handlers.continuations.propagateObjectReferences(result.state, references))
        val recorded = finished.copy(state = com.wingedsheep.engine.core.ControlHistory.record(finished.state, finished.events))
        val completed = runReplacementRiders(recorded, context)
        return if (context.deferGraveyardOrdering) completed
            else com.wingedsheep.engine.mechanics.GraveyardOrdering.finish(completed)
    }

    /**
     * A prevention effect that prevented damage during [result] may owe a result of its own
     * (Purity's life gain, Vigor's counters). Run it now, before the next instruction of the
     * resolving spell or ability — see [com.wingedsheep.engine.replacement.ReplacementRiders].
     */
    private fun runReplacementRiders(result: EffectResult, context: EffectContext): EffectResult {
        if (result.state.pendingReplacementRiders.isEmpty() || result.outcome !is Outcome.Done) return result
        val drained = com.wingedsheep.engine.replacement.ReplacementRiders.drain(result.state) { s, e, c ->
            execute(s, e, c.copy(resolutionDepth = context.resolutionDepth + 1))
        }
        return result.copy(state = drained.state, events = result.events + drained.events, outcome = drained.outcome)
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
