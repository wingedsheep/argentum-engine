package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RedirectDamageFromChosenSourceEffect
import kotlin.reflect.KClass

class RedirectDamageFromChosenSourceExecutor : EffectExecutor<RedirectDamageFromChosenSourceEffect> {
    override val effectType: KClass<RedirectDamageFromChosenSourceEffect> = RedirectDamageFromChosenSourceEffect::class

    override fun execute(state: GameState, effect: RedirectDamageFromChosenSourceEffect, context: EffectContext): EffectResult {
        val protected = context.resolveTarget(effect.protectedTarget, state) ?: return EffectResult.success(state)
        val recipient = context.resolveTarget(effect.redirectTo, state) ?: return EffectResult.success(state)
        val choices = damageSourceChoices(state, context)
        if (choices.isEmpty()) return EffectResult.success(state)
        val continuation = RedirectDamageSourceContinuation(
            context.controllerId, context.sourceId, protected, state.objectRef(protected),
            recipient, state.objectRef(recipient), effect.duration, choices, context.objectReferences
        )
        val decisionContext = DecisionContext(sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name })
        // Current battlefield/stack objects use the existing board/multi-zone selection flow.
        // A departed object (or two visits of one card) needs a distinct labelled source choice.
        val onBoard = choices.all { state.isCurrentObject(it.reference) } &&
            choices.map { it.reference.entityId }.distinct().size == choices.size
        val result = if (onBoard) state.suspendForDecision({ id -> SelectCardsDecision(
            id, context.controllerId, "Choose a source of damage", decisionContext,
            choices.map { it.reference.entityId }, 1, 1, useTargetingUI = true
        ) }, continuation) else state.suspendForDecision({ id -> ChooseOptionDecision(
            id, context.controllerId, "Choose a source of damage", decisionContext,
            choices.map { it.name + if (state.isCurrentObject(it.reference)) "" else " — departed source" },
            optionCardIds = choices.withIndex().filter { state.isCurrentObject(it.value.reference) }
                .associate { it.index to listOf(it.value.reference.entityId) }
        ) }, continuation)
        return EffectResult.from(result)
    }
}
