package com.wingedsheep.engine.handlers.effects

/**
 * Interface for grouping related effect executors into modules.
 *
 * Each module represents a category of effects (e.g., damage, life, drawing)
 * and provides all executors for that category. This enables modular organization
 * and reduces merge conflicts when adding new executors.
 */
interface ExecutorModule {
    /**
     * Returns all executors provided by this module.
     */
    fun executors(): List<EffectExecutor<*>>
}
