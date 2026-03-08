package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantKeywordToAttackersBlockedByEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantKeywordToAttackersBlockedByEffect.
 * Finds all attacking creatures whose BlockedComponent contains the target blocker,
 * and grants them the specified keyword via floating effects.
 */
class GrantKeywordToAttackersBlockedByExecutor : EffectExecutor<GrantKeywordToAttackersBlockedByEffect> {

    override val effectType: KClass<GrantKeywordToAttackersBlockedByEffect> =
        GrantKeywordToAttackersBlockedByEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordToAttackersBlockedByEffect,
        context: EffectContext
    ): ExecutionResult {
        val blockerId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Find all attackers that were blocked by this creature
        val attackerIds = state.findEntitiesWith<BlockedComponent>()
            .filter { (_, blockedComponent) -> blockerId in blockedComponent.blockerIds }
            .map { (entityId, _) -> entityId }

        if (attackerIds.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Create floating effects granting the keyword to each attacker
        val floatingEffects = attackerIds.map { attackerId ->
            ActiveFloatingEffect(
                id = EntityId.generate(),
                effect = FloatingEffectData(
                    layer = Layer.ABILITY,
                    sublayer = null,
                    modification = SerializableModification.GrantKeyword(effect.keyword),
                    affectedEntities = setOf(attackerId)
                ),
                duration = effect.duration,
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                controllerId = context.controllerId,
                timestamp = System.currentTimeMillis()
            )
        }

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffects
        )

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = attackerIds.mapNotNull { attackerId ->
            val cardComponent = state.getEntity(attackerId)?.get<CardComponent>() ?: return@mapNotNull null
            KeywordGrantedEvent(
                targetId = attackerId,
                targetName = cardComponent.name,
                keyword = effect.keyword.lowercase().replace('_', ' '),
                sourceName = sourceName
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
