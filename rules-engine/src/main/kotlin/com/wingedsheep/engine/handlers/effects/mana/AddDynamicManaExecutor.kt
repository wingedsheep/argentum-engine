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
 * Resolution branches on the size of [AddDynamicManaEffect.allowedColors]:
 *  - 1 color: add all mana as that color.
 *  - 2 colors: a single [ChooseNumberDecision] asks how much of the first color; the
 *    remainder goes to the second. Pushes [AddDynamicManaContinuation].
 *  - 3+ colors ("any combination of colors"): pip-by-pip [ChooseColorDecision] sequence.
 *    Pushes [AddManaPipsContinuation]; each resume adds one pip and re-pauses if more
 *    pips remain.
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

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // 3+ colors — pip-by-pip color choice ("any combination of colors")
        if (colors.size >= 3) {
            return pausePipDecision(
                state = state,
                playerId = context.controllerId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                remainingPips = amount,
                allowedColors = effect.allowedColors,
                restriction = effect.restriction,
                decisionHandler = decisionHandler
            )
        }

        // Two colors — ask the player how to split
        val firstColor = colors[0]
        val secondColor = colors[1]

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
        /**
         * Create a [ChooseColorDecision] over [allowedColors] and push an
         * [AddManaPipsContinuation] carrying the remaining pip count. Used both for the
         * initial pause (from [execute]) and for re-pausing between successive pips
         * (from the resumer).
         */
        fun pausePipDecision(
            state: GameState,
            playerId: com.wingedsheep.sdk.model.EntityId,
            sourceId: com.wingedsheep.sdk.model.EntityId?,
            sourceName: String?,
            remainingPips: Int,
            allowedColors: Set<Color>,
            restriction: ManaRestriction?,
            decisionHandler: DecisionHandler = DecisionHandler()
        ): EffectResult {
            val decisionResult = decisionHandler.createColorDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Choose a color of mana to add ($remainingPips remaining)",
                phase = DecisionPhase.RESOLUTION,
                availableColors = allowedColors
            )
            val continuation = AddManaPipsContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingPips = remainingPips,
                allowedColors = allowedColors,
                restriction = restriction
            )
            return EffectResult.paused(
                decisionResult.state.pushContinuation(continuation),
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

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
