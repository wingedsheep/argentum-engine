package com.wingedsheep.rulesengine.ecs.combat

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.layers.GameObjectView
import com.wingedsheep.rulesengine.ecs.layers.ModifierProvider
import com.wingedsheep.rulesengine.ecs.layers.StateProjector

/**
 * Projects damage events through prevention and modification effects.
 *
 * This implements MTG's damage replacement effects (Rule 614) and prevention
 * effects (Rule 615). It takes pending damage events and applies all active
 * prevention/modification effects to determine the final damage amounts.
 *
 * Common effects handled:
 * - Fog (prevent all combat damage)
 * - Protection from [X] (prevent damage from sources with that characteristic)
 * - Damage reduction (reduce damage by N)
 * - Damage shields (prevent the next N damage)
 * - Indestructible creatures can still receive damage (they just won't die from it)
 *
 * Usage:
 * ```kotlin
 * val projector = DamageEventProjector(state, effects)
 * val finalEvents = projector.project(pendingDamageEvents)
 * ```
 */
class DamageEventProjector(
    private val state: GameState,
    private val effects: List<DamagePreventionEffect> = emptyList(),
    private val modifierProvider: ModifierProvider? = null
) {
    private val projector: StateProjector by lazy {
        StateProjector.forState(state, modifierProvider)
    }

    /**
     * Apply all prevention effects to the pending damage events.
     *
     * @return The final damage events after all prevention effects
     */
    fun project(events: List<CombatDamageCalculator.PendingDamageEvent>): DamageProjectionResult {
        val projected = mutableListOf<CombatDamageCalculator.PendingDamageEvent>()
        val prevented = mutableListOf<PreventedDamage>()
        val consumedShields = mutableMapOf<EntityId, Int>()

        for (event in events) {
            val result = projectSingleEvent(event, consumedShields)
            when (result) {
                is SingleEventResult.Dealt -> projected.add(result.event)
                is SingleEventResult.Prevented -> prevented.add(result.prevention)
                is SingleEventResult.Reduced -> {
                    projected.add(result.event)
                    prevented.add(result.prevention)
                }
            }
        }

        return DamageProjectionResult(projected, prevented, consumedShields)
    }

    /**
     * Project a single damage event through prevention effects.
     */
    private fun projectSingleEvent(
        event: CombatDamageCalculator.PendingDamageEvent,
        consumedShields: MutableMap<EntityId, Int>
    ): SingleEventResult {
        val source = projector.getView(event.sourceId)
        val targetId = getTargetId(event)
        var remainingAmount = event.amount

        // Check each prevention effect
        for (effect in effects) {
            if (remainingAmount <= 0) break

            val result = applyEffect(effect, event, source, targetId, remainingAmount, consumedShields)
            remainingAmount = result.remainingDamage

            if (result.fullyPrevented) {
                return SingleEventResult.Prevented(
                    PreventedDamage(
                        sourceId = event.sourceId,
                        targetId = targetId,
                        originalAmount = event.amount,
                        preventedBy = effect.sourceId,
                        reason = effect.description
                    )
                )
            }
        }

        // Check for protection on the target
        if (remainingAmount > 0 && source != null) {
            val targetView = projector.getView(targetId)
            if (targetView != null && hasProtectionFrom(targetView, source)) {
                return SingleEventResult.Prevented(
                    PreventedDamage(
                        sourceId = event.sourceId,
                        targetId = targetId,
                        originalAmount = event.amount,
                        preventedBy = targetId, // Protection is on the target
                        reason = "Protection"
                    )
                )
            }
        }

        // Return result
        if (remainingAmount <= 0) {
            return SingleEventResult.Prevented(
                PreventedDamage(
                    sourceId = event.sourceId,
                    targetId = targetId,
                    originalAmount = event.amount,
                    preventedBy = null,
                    reason = "Damage prevention"
                )
            )
        } else if (remainingAmount < event.amount) {
            return SingleEventResult.Reduced(
                event = updateEventAmount(event, remainingAmount),
                prevention = PreventedDamage(
                    sourceId = event.sourceId,
                    targetId = targetId,
                    originalAmount = event.amount - remainingAmount,
                    preventedBy = null,
                    reason = "Damage reduction"
                )
            )
        }

        return SingleEventResult.Dealt(event)
    }

    /**
     * Apply a single prevention effect to damage.
     */
    private fun applyEffect(
        effect: DamagePreventionEffect,
        event: CombatDamageCalculator.PendingDamageEvent,
        source: GameObjectView?,
        targetId: EntityId,
        currentAmount: Int,
        consumedShields: MutableMap<EntityId, Int>
    ): EffectApplicationResult {
        // Check if effect applies to this damage
        if (!effectApplies(effect, event, source, targetId)) {
            return EffectApplicationResult(currentAmount, false)
        }

        return when (effect) {
            is DamagePreventionEffect.PreventAll -> {
                EffectApplicationResult(0, true)
            }

            is DamagePreventionEffect.PreventAllCombatDamage -> {
                if (event.isCombatDamage) {
                    EffectApplicationResult(0, true)
                } else {
                    EffectApplicationResult(currentAmount, false)
                }
            }

            is DamagePreventionEffect.PreventCombatDamageToPlayer -> {
                if (event is CombatDamageCalculator.PendingDamageEvent.ToPlayer &&
                    event.targetPlayerId == effect.playerId
                ) {
                    EffectApplicationResult(0, true)
                } else {
                    EffectApplicationResult(currentAmount, false)
                }
            }

            is DamagePreventionEffect.PreventCombatDamageToCreatures -> {
                if (event is CombatDamageCalculator.PendingDamageEvent.ToCreature) {
                    EffectApplicationResult(0, true)
                } else {
                    EffectApplicationResult(currentAmount, false)
                }
            }

            is DamagePreventionEffect.PreventNextN -> {
                val alreadyConsumed = consumedShields[effect.sourceId] ?: 0
                val remaining = effect.amount - alreadyConsumed

                if (remaining <= 0) {
                    EffectApplicationResult(currentAmount, false)
                } else {
                    val prevented = minOf(remaining, currentAmount)
                    consumedShields[effect.sourceId] = alreadyConsumed + prevented
                    val newAmount = currentAmount - prevented
                    EffectApplicationResult(newAmount, newAmount <= 0)
                }
            }

            is DamagePreventionEffect.ReduceBy -> {
                val reduced = (currentAmount - effect.amount).coerceAtLeast(0)
                EffectApplicationResult(reduced, reduced <= 0)
            }

            is DamagePreventionEffect.PreventFromSource -> {
                if (event.sourceId == effect.preventedSourceId) {
                    EffectApplicationResult(0, true)
                } else {
                    EffectApplicationResult(currentAmount, false)
                }
            }

            is DamagePreventionEffect.PreventToTarget -> {
                if (targetId == effect.protectedTargetId) {
                    EffectApplicationResult(0, true)
                } else {
                    EffectApplicationResult(currentAmount, false)
                }
            }
        }
    }

    /**
     * Check if an effect applies to this specific damage event.
     */
    private fun effectApplies(
        effect: DamagePreventionEffect,
        event: CombatDamageCalculator.PendingDamageEvent,
        source: GameObjectView?,
        targetId: EntityId
    ): Boolean {
        // Check combat-only effects
        if (effect.combatDamageOnly && !event.isCombatDamage) {
            return false
        }

        // Check source filters
        effect.sourceFilter?.let { filter ->
            if (source == null) return false
            if (!matchesFilter(source, filter)) return false
        }

        // Check target filters
        effect.targetFilter?.let { filter ->
            val target = projector.getView(targetId) ?: return false
            if (!matchesFilter(target, filter)) return false
        }

        return true
    }

    /**
     * Check if a game object matches a damage filter.
     */
    private fun matchesFilter(view: GameObjectView, filter: DamageFilter): Boolean {
        return when (filter) {
            is DamageFilter.ByColor -> filter.color in view.colors
            is DamageFilter.ByType -> filter.type in view.types
            is DamageFilter.ByController -> view.controllerId == filter.controllerId
            is DamageFilter.IsCreature -> view.isCreature
            is DamageFilter.IsPlayer -> false // GameObjectView is not a player
            is DamageFilter.Any -> true
            is DamageFilter.And -> filter.filters.all { matchesFilter(view, it) }
            is DamageFilter.Or -> filter.filters.any { matchesFilter(view, it) }
            is DamageFilter.Not -> !matchesFilter(view, filter.filter)
        }
    }

    /**
     * Check if a creature has protection from the source.
     */
    private fun hasProtectionFrom(target: GameObjectView, source: GameObjectView): Boolean {
        if (!target.hasKeyword(Keyword.PROTECTION)) {
            return false
        }

        // Basic protection checks (would need more sophisticated tracking
        // of what the protection is from)
        // For now, check color-based protection
        // A full implementation would store the protection characteristics
        return false // Placeholder - full implementation needs protection tracking
    }

    /**
     * Get the target entity ID from a damage event.
     */
    private fun getTargetId(event: CombatDamageCalculator.PendingDamageEvent): EntityId {
        return when (event) {
            is CombatDamageCalculator.PendingDamageEvent.ToPlayer -> event.targetPlayerId
            is CombatDamageCalculator.PendingDamageEvent.ToPlaneswalker -> event.targetPlaneswalker
            is CombatDamageCalculator.PendingDamageEvent.ToCreature -> event.targetCreatureId
        }
    }

    /**
     * Create a new damage event with updated amount.
     */
    private fun updateEventAmount(
        event: CombatDamageCalculator.PendingDamageEvent,
        newAmount: Int
    ): CombatDamageCalculator.PendingDamageEvent {
        return when (event) {
            is CombatDamageCalculator.PendingDamageEvent.ToPlayer ->
                event.copy(amount = newAmount)
            is CombatDamageCalculator.PendingDamageEvent.ToPlaneswalker ->
                event.copy(amount = newAmount)
            is CombatDamageCalculator.PendingDamageEvent.ToCreature ->
                event.copy(amount = newAmount)
        }
    }

    private sealed interface SingleEventResult {
        data class Dealt(val event: CombatDamageCalculator.PendingDamageEvent) : SingleEventResult
        data class Prevented(val prevention: PreventedDamage) : SingleEventResult
        data class Reduced(
            val event: CombatDamageCalculator.PendingDamageEvent,
            val prevention: PreventedDamage
        ) : SingleEventResult
    }

    private data class EffectApplicationResult(
        val remainingDamage: Int,
        val fullyPrevented: Boolean
    )

    companion object {
        /**
         * Create a projector with no prevention effects.
         * All damage passes through unchanged.
         */
        fun passthrough(state: GameState): DamageEventProjector {
            return DamageEventProjector(state, emptyList())
        }

        /**
         * Create a Fog-style projector that prevents all combat damage.
         */
        fun fog(state: GameState, sourceId: EntityId): DamageEventProjector {
            return DamageEventProjector(
                state,
                listOf(DamagePreventionEffect.PreventAllCombatDamage(sourceId, "Fog"))
            )
        }
    }
}

/**
 * Result of projecting damage through prevention effects.
 */
data class DamageProjectionResult(
    /** Damage events that will actually be dealt */
    val finalEvents: List<CombatDamageCalculator.PendingDamageEvent>,
    /** Damage that was prevented */
    val preventedDamage: List<PreventedDamage>,
    /** How much of each damage shield was consumed */
    val consumedShields: Map<EntityId, Int>
) {
    val totalDamageDealt: Int
        get() = finalEvents.sumOf { it.amount }

    val totalDamagePrevented: Int
        get() = preventedDamage.sumOf { it.originalAmount }

    val hasPrevention: Boolean
        get() = preventedDamage.isNotEmpty()
}

/**
 * Information about damage that was prevented.
 */
data class PreventedDamage(
    val sourceId: EntityId,
    val targetId: EntityId,
    val originalAmount: Int,
    val preventedBy: EntityId?,
    val reason: String
)

/**
 * Represents a damage prevention or modification effect.
 */
sealed class DamagePreventionEffect {
    abstract val sourceId: EntityId
    abstract val description: String
    open val combatDamageOnly: Boolean = false
    open val sourceFilter: DamageFilter? = null
    open val targetFilter: DamageFilter? = null

    /**
     * Prevent all damage (to everything, from everything).
     */
    data class PreventAll(
        override val sourceId: EntityId,
        override val description: String = "Prevent all damage"
    ) : DamagePreventionEffect()

    /**
     * Prevent all combat damage this turn (Fog effect).
     */
    data class PreventAllCombatDamage(
        override val sourceId: EntityId,
        override val description: String = "Prevent all combat damage"
    ) : DamagePreventionEffect() {
        override val combatDamageOnly = true
    }

    /**
     * Prevent all combat damage that would be dealt to a player.
     */
    data class PreventCombatDamageToPlayer(
        override val sourceId: EntityId,
        val playerId: EntityId,
        override val description: String = "Prevent combat damage to player"
    ) : DamagePreventionEffect() {
        override val combatDamageOnly = true
    }

    /**
     * Prevent all combat damage that would be dealt to creatures.
     */
    data class PreventCombatDamageToCreatures(
        override val sourceId: EntityId,
        override val description: String = "Prevent combat damage to creatures"
    ) : DamagePreventionEffect() {
        override val combatDamageOnly = true
    }

    /**
     * Prevent the next N damage that would be dealt.
     * This is a damage shield that gets consumed.
     */
    data class PreventNextN(
        override val sourceId: EntityId,
        val amount: Int,
        override val description: String = "Prevent next $amount damage"
    ) : DamagePreventionEffect()

    /**
     * Reduce damage by a fixed amount.
     */
    data class ReduceBy(
        override val sourceId: EntityId,
        val amount: Int,
        override val description: String = "Reduce damage by $amount"
    ) : DamagePreventionEffect()

    /**
     * Prevent all damage from a specific source.
     */
    data class PreventFromSource(
        override val sourceId: EntityId,
        val preventedSourceId: EntityId,
        override val description: String = "Prevent damage from source"
    ) : DamagePreventionEffect()

    /**
     * Prevent all damage to a specific target.
     */
    data class PreventToTarget(
        override val sourceId: EntityId,
        val protectedTargetId: EntityId,
        override val description: String = "Prevent damage to target"
    ) : DamagePreventionEffect()
}

/**
 * Filters for matching damage sources or targets.
 */
sealed interface DamageFilter {
    data class ByColor(val color: Color) : DamageFilter
    data class ByType(val type: com.wingedsheep.rulesengine.core.CardType) : DamageFilter
    data class ByController(val controllerId: EntityId) : DamageFilter
    data object IsCreature : DamageFilter
    data object IsPlayer : DamageFilter
    data object Any : DamageFilter
    data class And(val filters: List<DamageFilter>) : DamageFilter
    data class Or(val filters: List<DamageFilter>) : DamageFilter
    data class Not(val filter: DamageFilter) : DamageFilter
}
