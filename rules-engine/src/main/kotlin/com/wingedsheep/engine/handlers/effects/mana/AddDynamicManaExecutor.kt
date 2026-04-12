package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import kotlin.reflect.KClass

/**
 * Executor for AddDynamicManaEffect.
 * "Add X mana in any combination of {R} and/or {G}."
 *
 * When there is only one allowed color, adds all mana as that color.
 * When there are multiple allowed colors, presents a [ChooseNumberDecision]
 * asking how much of the first color to add (the rest goes to the second color).
 * Pushes an [AddDynamicManaContinuation] for the decision response.
 */
class AddDynamicManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<AddDynamicManaEffect> {

    override val effectType: KClass<AddDynamicManaEffect> = AddDynamicManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddDynamicManaEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amountSource, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val colors = effect.allowedColors.toList().sorted()

        // Single color — just add it all
        if (colors.size <= 1) {
            val color = colors.firstOrNull() ?: return EffectResult.success(state)
            val newState = addMana(state, context.controllerId, mapOf(color to amount), effect.restriction)
            return EffectResult.success(newState)
        }

        // Two+ colors — ask the player how to split
        val firstColor = colors[0]
        val secondColor = colors[1]

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose how much {${firstColor.symbol}} mana to add (rest will be {${secondColor.symbol}}). Total: $amount",
            minValue = 0,
            maxValue = amount,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = AddDynamicManaContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            totalAmount = amount,
            firstColor = firstColor,
            secondColor = secondColor,
            restriction = effect.restriction
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    companion object {
        fun addMana(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId, amounts: Map<Color, Int>, restriction: ManaRestriction? = null): GameState {
            return state.updateEntity(playerId) { container ->
                var manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                for ((color, amount) in amounts) {
                    if (amount > 0) {
                        manaPool = if (restriction != null) {
                            manaPool.addRestricted(color, amount, restriction)
                        } else {
                            manaPool.add(color, amount)
                        }
                    }
                }
                container.with(manaPool)
            }
        }
    }
}
