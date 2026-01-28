package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.DestroyAllEffect
import com.wingedsheep.sdk.targeting.PermanentTargetFilter
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllEffect.
 * Unified executor that handles all "destroy all X" patterns.
 *
 * Examples:
 * - DestroyAllEffect(Land) -> "Destroy all lands"
 * - DestroyAllEffect(Creature, noRegenerate = true) -> Wrath of God
 * - DestroyAllEffect(And(Creature, WithColor(WHITE))) -> Virtue's Ruin
 */
class DestroyAllExecutor : EffectExecutor<DestroyAllEffect> {

    override val effectType: KClass<DestroyAllEffect> = DestroyAllEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // Note: noRegenerate flag is stored but not yet enforced.
        // Regeneration support will be added in a future update.
        // When implemented, this executor will need to prevent regeneration
        // replacement effects from being applied when noRegenerate is true.

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            // Check if this permanent matches the filter
            if (!matchesFilter(effect.filter, cardComponent, entityController, context.controllerId)) {
                continue
            }

            val result = destroyPermanent(newState, entityId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Check if a permanent matches the given filter.
     */
    private fun matchesFilter(
        filter: PermanentTargetFilter,
        cardComponent: CardComponent,
        entityController: com.wingedsheep.sdk.model.EntityId?,
        controllerId: com.wingedsheep.sdk.model.EntityId
    ): Boolean {
        return when (filter) {
            is PermanentTargetFilter.Any -> true
            is PermanentTargetFilter.YouControl -> entityController == controllerId
            is PermanentTargetFilter.OpponentControls -> entityController != controllerId
            is PermanentTargetFilter.Creature -> cardComponent.typeLine.isCreature
            is PermanentTargetFilter.Artifact -> cardComponent.typeLine.isArtifact
            is PermanentTargetFilter.Enchantment -> cardComponent.typeLine.isEnchantment
            is PermanentTargetFilter.Land -> cardComponent.typeLine.isLand
            is PermanentTargetFilter.NonCreature -> !cardComponent.typeLine.isCreature
            is PermanentTargetFilter.NonLand -> !cardComponent.typeLine.isLand
            is PermanentTargetFilter.CreatureOrLand -> cardComponent.typeLine.isCreature || cardComponent.typeLine.isLand
            is PermanentTargetFilter.WithColor -> cardComponent.colors.contains(filter.color)
            is PermanentTargetFilter.WithSubtype -> cardComponent.typeLine.hasSubtype(filter.subtype)
            is PermanentTargetFilter.And -> filter.filters.all {
                matchesFilter(it, cardComponent, entityController, controllerId)
            }
        }
    }
}
