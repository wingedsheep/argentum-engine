package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantPlayerProtectionEffect
import com.wingedsheep.sdk.scripting.effects.protectionDescription
import kotlin.reflect.KClass

/**
 * Executor for [GrantPlayerProtectionEffect] — grants a player protection from a
 * [com.wingedsheep.sdk.scripting.ProtectionScope] for a duration (The One Ring's
 * "you gain protection from everything until your next turn", CR 702.16).
 *
 * Adds (or merges into) a [PlayerProtectionComponent] on the target player. Multiple
 * grants append to the existing scope list. The targeting/damage systems consult the
 * component via [com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules].
 */
class GrantPlayerProtectionExecutor : EffectExecutor<GrantPlayerProtectionEffect> {

    override val effectType: KClass<GrantPlayerProtectionEffect> = GrantPlayerProtectionEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayerProtectionEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for player protection grant")

        if (!state.turnOrder.contains(targetId)) {
            return EffectResult.error(state, "GrantPlayerProtection target must be a player")
        }

        val removeOn = when (effect.duration) {
            is Duration.Permanent -> PlayerEffectRemoval.Permanent
            is Duration.EndOfTurn -> PlayerEffectRemoval.EndOfTurn
            else -> PlayerEffectRemoval.UntilYourNextTurn
        }

        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<PlayerProtectionComponent>()
            val mergedScopes = (existing?.scopes ?: emptyList()) + effect.scope
            container.with(PlayerProtectionComponent(scopes = mergedScopes, removeOn = removeOn))
        }

        val playerName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Player"
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = playerName,
                keyword = "Protection from ${effect.scope.protectionDescription()}",
                sourceName = sourceName
            )
        )
        return EffectResult.success(newState, events)
    }
}
