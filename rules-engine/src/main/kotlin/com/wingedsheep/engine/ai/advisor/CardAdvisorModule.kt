package com.wingedsheep.engine.ai.advisor

/**
 * Groups [CardAdvisor] registrations by set or theme.
 *
 * Mirrors the engine's [ActionHandlerModule] / [ExecutorModule] pattern:
 * each module registers its advisors into the shared [CardAdvisorRegistry].
 *
 * Example:
 * ```
 * class ScourgeAdvisorModule : CardAdvisorModule {
 *     override fun register(registry: CardAdvisorRegistry) {
 *         registry.register(WingShards)
 *         registry.register(DiviningWitch)
 *     }
 * }
 * ```
 */
interface CardAdvisorModule {
    fun register(registry: CardAdvisorRegistry)
}
