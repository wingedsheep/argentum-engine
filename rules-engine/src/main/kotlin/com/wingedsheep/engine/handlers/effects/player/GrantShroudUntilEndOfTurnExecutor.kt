package com.wingedsheep.engine.handlers.effects.player

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
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantShroudUntilEndOfTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantShroudUntilEndOfTurnEffect.
 * Grants shroud until end of turn to a target entity.
 *
 * - For player targets: adds PlayerShroudComponent with EndOfTurn removal
 * - For permanent targets: creates a floating effect for the Shroud keyword
 */
class GrantShroudUntilEndOfTurnExecutor : EffectExecutor<GrantShroudUntilEndOfTurnEffect> {

    override val effectType: KClass<GrantShroudUntilEndOfTurnEffect> = GrantShroudUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: GrantShroudUntilEndOfTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for shroud grant")

        // Check if target is a player
        if (state.turnOrder.contains(targetId)) {
            val newState = state.updateEntity(targetId) { container ->
                container.with(PlayerShroudComponent(removeOn = PlayerEffectRemoval.EndOfTurn))
            }
            return ExecutionResult.success(newState)
        }

        // Target is a permanent — grant shroud keyword via floating effect
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.GrantKeyword(Keyword.SHROUD.name),
                affectedEntities = setOf(targetId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                keyword = Keyword.SHROUD.displayName,
                sourceName = sourceName
            )
        )

        return ExecutionResult.success(newState, events)
    }
}
