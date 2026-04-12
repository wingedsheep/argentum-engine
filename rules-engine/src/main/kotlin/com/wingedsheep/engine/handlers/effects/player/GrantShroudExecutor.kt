package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantShroudEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantShroudEffect.
 * Grants shroud to a target entity for the specified duration.
 *
 * - For player targets: adds PlayerShroudComponent with appropriate removal timing
 * - For permanent targets: creates a floating effect for the Shroud keyword
 */
class GrantShroudExecutor : EffectExecutor<GrantShroudEffect> {

    override val effectType: KClass<GrantShroudEffect> = GrantShroudEffect::class

    override fun execute(
        state: GameState,
        effect: GrantShroudEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for shroud grant")

        // Check if target is a player
        if (state.turnOrder.contains(targetId)) {
            val removeOn = when (effect.duration) {
                is Duration.Permanent -> PlayerEffectRemoval.Permanent
                else -> PlayerEffectRemoval.EndOfTurn
            }
            val newState = state.updateEntity(targetId) { container ->
                container.with(PlayerShroudComponent(removeOn = removeOn))
            }
            return EffectResult.success(newState)
        }

        // Target is a permanent — grant shroud keyword via floating effect
        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(Keyword.SHROUD.name),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
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

        return EffectResult.success(newState, events)
    }
}
