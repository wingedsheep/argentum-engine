package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Registry that maps effect types to their handlers.
 *
 * This is the central registry for effect execution. When a new effect type
 * is added, a corresponding handler is registered here.
 *
 * Usage:
 * ```kotlin
 * val registry = EffectHandlerRegistry.default()
 * val result = registry.execute(state, effect, context)
 * ```
 */
class EffectHandlerRegistry private constructor(
    private val handlers: Map<KClass<out Effect>, EffectHandler<*>>
) {

    /**
     * Execute an effect using the registered handler.
     *
     * @throws IllegalStateException if no handler is registered for the effect type
     */
    fun execute(
        state: GameState,
        effect: Effect,
        context: ExecutionContext
    ): ExecutionResult {
        val handler = handlers[effect::class]
            ?: throw IllegalStateException("No handler registered for effect type: ${effect::class.qualifiedName}")

        @Suppress("UNCHECKED_CAST")
        return (handler as EffectHandler<Effect>).execute(state, effect, context)
    }

    /**
     * Check if a handler is registered for the given effect type.
     */
    fun hasHandler(effectClass: KClass<out Effect>): Boolean =
        handlers.containsKey(effectClass)

    /**
     * Get the number of registered handlers.
     */
    val size: Int get() = handlers.size

    /**
     * Builder for creating a registry.
     */
    class Builder {
        private val handlers = mutableMapOf<KClass<out Effect>, EffectHandler<*>>()

        /**
         * Register a handler for its effect type.
         */
        fun <T : Effect> register(handler: EffectHandler<T>): Builder {
            handlers[handler.effectClass] = handler
            return this
        }

        /**
         * Register multiple handlers.
         */
        fun registerAll(vararg handlers: EffectHandler<*>): Builder {
            handlers.forEach { handler ->
                this.handlers[handler.effectClass] = handler
            }
            return this
        }

        /**
         * Build the registry.
         */
        fun build(): EffectHandlerRegistry = EffectHandlerRegistry(handlers.toMap())
    }

    companion object {
        /**
         * Create a new builder.
         */
        fun builder(): Builder = Builder()

        /**
         * Create the default registry with all standard handlers.
         *
         * Composite handlers need to recursively call the registry,
         * so we use lazy initialization to allow self-reference.
         */
        fun default(): EffectHandlerRegistry {
            // Create a holder for the registry reference
            lateinit var registry: EffectHandlerRegistry

            // Create composite handlers with lazy reference to the registry
            val compositeHandler = CompositeHandler { effect, state, context ->
                registry.execute(state, effect, context)
            }
            val conditionalHandler = ConditionalHandler { effect, state, context ->
                registry.execute(state, effect, context)
            }

            registry = builder()
                // Life effects
                .register(GainLifeHandler())
                .register(LoseLifeHandler())
                // Damage effects
                .register(DealDamageHandler())
                .register(DealDamageToAllCreaturesHandler())
                .register(DealDamageToAllHandler())
                .register(DrainHandler())
                // Card drawing effects
                .register(DrawCardsHandler())
                .register(DiscardCardsHandler())
                .register(ReturnFromGraveyardHandler())
                // Destruction/removal effects
                .register(DestroyHandler())
                .register(ExileHandler())
                .register(ReturnToHandHandler())
                // Tap/untap effects
                .register(TapUntapHandler())
                // Stat modification effects
                .register(ModifyStatsHandler())
                .register(AddCountersHandler())
                // Mana effects
                .register(AddManaHandler())
                .register(AddColorlessManaHandler())
                // Token effects
                .register(CreateTokenHandler())
                // Composite effects (use pre-created handlers with lazy reference)
                .register(compositeHandler)
                .register(conditionalHandler)
                // Library effects
                .register(ShuffleIntoLibraryHandler())
                .register(LookAtTopCardsHandler())
                .register(ShuffleLibraryHandler())
                .register(SearchLibraryHandler())
                .register(PutOnTopOfLibraryHandler())
                // Combat effects
                .register(MustBeBlockedHandler())
                .register(GrantKeywordUntilEndOfTurnHandler())
                // Mass destruction effects
                .register(DestroyAllLandsHandler())
                .register(DestroyAllCreaturesHandler())
                .register(DestroyAllLandsOfTypeHandler())
                // Sacrifice effects
                .register(SacrificeUnlessHandler())
                // Wheel effects
                .register(WheelHandler())
                .build()

            return registry
        }
    }
}
