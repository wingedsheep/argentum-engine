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
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantEvasionKeywordEffect] — grants a protection-style evasion keyword
 * (shroud or hexproof) to a player, creature, or planeswalker for the specified duration.
 *
 * - For player targets: adds the matching player protection component (players can't carry a
 *   keyword via the projection layer), with removal timing derived from the duration.
 * - For permanent targets: creates a Layer-6 floating effect granting the keyword — identical
 *   to what `GrantKeyword` produces.
 *
 * Unifies the former `GrantShroudEffect` / `GrantHexproofEffect` pair (SDK effect-atom audit,
 * backlog/2026-06-26 roadmap item #4).
 */
class GrantEvasionKeywordExecutor : EffectExecutor<GrantEvasionKeywordEffect> {

    override val effectType: KClass<GrantEvasionKeywordEffect> = GrantEvasionKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: GrantEvasionKeywordEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for ${effect.keyword.displayName.lowercase()} grant")

        // Player target — players can't hold a keyword via projection; attach the component.
        if (state.turnOrder.contains(targetId)) {
            val removeOn = when (effect.duration) {
                is Duration.Permanent -> PlayerEffectRemoval.Permanent
                else -> PlayerEffectRemoval.EndOfTurn
            }
            val component = when (effect.keyword) {
                Keyword.SHROUD -> PlayerShroudComponent(removeOn = removeOn)
                Keyword.HEXPROOF -> PlayerHexproofComponent(removeOn = removeOn)
                else -> return EffectResult.error(
                    state,
                    "Keyword ${effect.keyword.displayName} cannot be granted to a player"
                )
            }
            val newState = state.updateEntity(targetId) { container -> container.with(component) }
            return EffectResult.success(newState)
        }

        // Permanent target — grant the keyword via a floating effect.
        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(effect.keyword.name),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                keyword = effect.keyword.displayName,
                sourceName = sourceName
            )
        )

        return EffectResult.success(newState, events)
    }
}
