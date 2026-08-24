package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PayAnyAmountOfLifeAsEntersContinuation
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.EnteredWithValueComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PayAnyAmountOfLifeAsEntersEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [PayAnyAmountOfLifeAsEntersEffect] — "as this creature enters, pay any amount of
 * life", bounded by a dynamic ceiling (Nameless Race).
 *
 * Prompts the controller with a single [ChooseNumberDecision] over `0..min(ceiling, life)`, then
 * (on resume) pays that much life and stamps the amount on the entering permanent as
 * [EnteredWithValueComponent], which its characteristic-defining P/T reads back during projection.
 *
 * A ceiling that resolves to 0 records 0 without prompting: there is no choice to make, and asking
 * would be a decision with one legal answer. The life total is a second, independent bound — a
 * player can't pay life they don't have.
 */
class PayAnyAmountOfLifeAsEntersExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<PayAnyAmountOfLifeAsEntersEffect> {

    override val effectType: KClass<PayAnyAmountOfLifeAsEntersEffect> =
        PayAnyAmountOfLifeAsEntersEffect::class

    override fun execute(
        state: GameState,
        effect: PayAnyAmountOfLifeAsEntersEffect,
        context: EffectContext
    ): EffectResult {
        val permanentId = context.sourceId
            ?: return EffectResult.error(state, "No entering permanent for pay-any-amount-of-life")

        val ceiling = amountEvaluator.evaluate(state, effect.maxAmount, context)
        val life = state.lifeTotal(context.controllerId)
        val max = minOf(maxOf(ceiling, 0), maxOf(life, 0))
        if (max <= 0) {
            return EffectResult.success(recordValue(state, permanentId, 0))
        }

        val permanentName = state.getEntity(permanentId)?.get<CardComponent>()?.name ?: "it"
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Pay how much life as $permanentName enters? (0-$max)",
            context = DecisionContext(
                sourceId = permanentId,
                sourceName = permanentName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = max
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(
                PayAnyAmountOfLifeAsEntersContinuation(
                    decisionId = decisionId,
                    permanentId = permanentId,
                    controllerId = context.controllerId
                )
            )

        return EffectResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = context.controllerId,
                    decisionType = "CHOOSE_NUMBER",
                    prompt = decision.prompt
                )
            )
        )
    }

    companion object {
        /** Stamp [value] on [permanentId] so its characteristic-defining P/T can read it back. */
        fun recordValue(state: GameState, permanentId: EntityId, value: Int): GameState =
            state.updateEntity(permanentId) { it.with(EnteredWithValueComponent(value)) }
    }
}
