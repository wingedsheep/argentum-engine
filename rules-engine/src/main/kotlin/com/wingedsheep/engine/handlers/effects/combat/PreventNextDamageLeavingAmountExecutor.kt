package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageLeavingAmountEffect
import kotlin.reflect.KClass

class PreventNextDamageLeavingAmountExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<PreventNextDamageLeavingAmountEffect> {
    override val effectType: KClass<PreventNextDamageLeavingAmountEffect> = PreventNextDamageLeavingAmountEffect::class

    override fun execute(state: GameState, effect: PreventNextDamageLeavingAmountEffect, context: EffectContext): EffectResult {
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)
        val predicateContext = PredicateContext(controllerId = context.controllerId, sourceId = context.sourceId)
        val choices = damageSourceChoices(state, context).filter { choice ->
            state.isCurrentObject(choice.reference) && amountEvaluator.predicates.matches(
                state, state.projectedState, choice.reference.entityId, effect.eligibleSource, predicateContext
            )
        }
        if (choices.isEmpty()) return EffectResult.success(state)
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val continuation = PreventNextDamageLeavingAmountContinuation(
            context = context,
            targetId = targetId,
            amountToLeave = amountEvaluator.evaluate(state, effect.amountToLeave, context).coerceAtLeast(0),
            eligibleSource = effect.eligibleSource,
            scope = effect.scope,
            duration = effect.duration,
            choices = choices
        )
        return EffectResult.from(state.suspendForDecision({ id ->
            SelectCardsDecision(
                id = id, playerId = context.controllerId, prompt = "Choose a source of damage",
                context = DecisionContext(sourceId = context.sourceId, sourceName = sourceName),
                options = choices.map { it.reference.entityId }, minSelections = 1, maxSelections = 1,
                useTargetingUI = true
            )
        }, continuation))
    }
}
