package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnSameNamedFromGraveyardEffect
import kotlin.reflect.KClass

/**
 * Resolve the target graveyard card, then return it together with every other card sharing its
 * name in the controller's graveyard to the battlefield tapped under the controller's control.
 * Each card is moved through [ZoneTransitionService] so enters-with-counters and other entry
 * replacements still apply.
 */
class ReturnSameNamedFromGraveyardExecutor(private val zones: ZoneTransitionService) : EffectExecutor<ReturnSameNamedFromGraveyardEffect> {

    override val effectType: KClass<ReturnSameNamedFromGraveyardEffect> =
        ReturnSameNamedFromGraveyardEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnSameNamedFromGraveyardEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        val targetName = state.getEntity(targetId)?.get<CardComponent>()?.name
            ?: return EffectResult.success(state)
        val controllerId = context.controllerId

        // "your graveyard" — only cards in the controller's graveyard sharing the target's name
        // (the target itself is one of them).
        val graveyardKey = ZoneKey(controllerId, Zone.GRAVEYARD)
        val toReturn = state.getGraveyard(controllerId).filter { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == targetName
        }
        if (toReturn.isEmpty()) return EffectResult.success(state)
        // "If this would enter, you may [pay] instead" (EntersOnlyIfPaid) — asked before anything moves.
        com.wingedsheep.engine.handlers.effects.EffectEntryPayments.prepare(
            state, effect, context, toReturn.associateWith { controllerId }, zones.cardRegistry, zones.predicateEvaluator
        )?.let { return it }

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (id in toReturn) {
            val result = zones.moveToZone(
                newState,
                id,
                Zone.BATTLEFIELD,
                ZoneEntryOptions(controllerId = controllerId, tapped = true,
                    paidToEnter = com.wingedsheep.engine.handlers.effects.EffectEntryPayments.paid(context, id)),
                graveyardKey
            )
            newState = result.state
            events.addAll(result.events)
        }
        return EffectResult.success(newState, events)
    }
}
