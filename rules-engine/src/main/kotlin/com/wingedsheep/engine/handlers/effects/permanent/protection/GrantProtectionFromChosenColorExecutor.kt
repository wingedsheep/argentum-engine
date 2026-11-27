package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.handlers.effects.player.grantPlayerProtection
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantProtectionFromChosenColorEffect] — reads `chosenColor` from
 * the effect context (set by the surrounding `ChooseColorThen` resumer) and
 * grants `PROTECTION_FROM_<COLOR>` to the target — or, when the target is a player, player
 * protection from that color.
 */
class GrantProtectionFromChosenColorExecutor : EffectExecutor<GrantProtectionFromChosenColorEffect> {

    override val effectType: KClass<GrantProtectionFromChosenColorEffect> =
        GrantProtectionFromChosenColorEffect::class

    override fun execute(
        state: GameState,
        effect: GrantProtectionFromChosenColorEffect,
        context: EffectContext
    ): EffectResult {
        val color = context.chosenColor
            ?: return EffectResult.error(state, "GrantProtectionFromChosenColor requires a chosen color in context")

        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for protection grant")

        // A player target ("you … gain protection from the color of your choice") gets player
        // protection (CR 702.16b, 702.16e), not a floating keyword — the targeting, damage and attach
        // checks read it through PlayerProtectionRules.
        if (targetId in state.turnOrder) {
            return grantPlayerProtection(state, targetId, ProtectionScope.Color(color), effect.duration, context)
        }

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target no longer exists")
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantProtectionFromColor(color.name),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        val displayName = nameVisibleToAll(state, targetId, cardComponent.name)
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = displayName,
                keyword = "Protection from ${color.displayName.lowercase()}",
                sourceName = sourceName
            )
        )

        return EffectResult.success(newState, events)
    }
}
