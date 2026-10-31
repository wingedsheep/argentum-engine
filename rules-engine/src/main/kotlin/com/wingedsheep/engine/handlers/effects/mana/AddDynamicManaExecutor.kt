package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.mechanics.mana.ChosenTypeManaRestriction
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
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
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
 *  - [AddDynamicManaEffect.differentColors] ("two mana of different colors"): the pip-by-pip
 *    sequence regardless of palette size, each chosen color removed from the next prompt.
 */
class AddDynamicManaExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val amountEvaluator: DynamicAmountEvaluator
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

        val restriction = ChosenTypeManaRestriction.bind(effect.restriction, state, context.sourceId)
        val unit = context.manaUnitMultiplier
        val colors = effect.allowedColors.toList().sorted()

        // Single color — just add it all
        if (colors.size <= 1) {
            val color = colors.firstOrNull() ?: return EffectResult.success(state)
            val newState = addMana(state, context.controllerId, mapOf(color to (if (effect.differentColors) 1 else amount) * unit), restriction, effect.expiry)
            return EffectResult.success(newState)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // 3+ colors — pip-by-pip color choice ("any combination of colors"). "Of different
        // colors" always goes pip-by-pip too, each pick leaving the next prompt's palette; it can
        // make at most one unit per allowed color.
        if (colors.size >= 3 || effect.differentColors) {
            return pausePipDecision(
                state = state,
                playerId = context.controllerId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                remainingPips = if (effect.differentColors) minOf(amount, colors.size) else amount,
                allowedColors = effect.allowedColors,
                restriction = restriction,
                expiry = effect.expiry,
                decisionHandler = decisionHandler,
                unitMultiplier = unit,
                differentColors = effect.differentColors,
            )
        }

        // Two colors — ask the player how to split
        val firstColor = colors[0]
        val secondColor = colors[1]

        val continuation = AddDynamicManaContinuation(
            playerId = context.controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            totalAmount = amount,
            firstColor = firstColor,
            secondColor = secondColor,
            restriction = restriction,
            expiry = effect.expiry,
            unitMultiplier = unit,
        )

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose how much {${firstColor.symbol}} mana to add (rest will be {${secondColor.symbol}}). Total: $amount" +
                multipliedSuffix(unit),
            minValue = 0,
            maxValue = amount,
            phase = DecisionPhase.RESOLUTION,
            answer = continuation
        )

        return EffectResult.propagatePause(
            decisionResult.state,
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
            expiry: ManaExpiry = ManaExpiry.END_OF_TURN,
            decisionHandler: DecisionHandler = DecisionHandler(),
            unitMultiplier: Int = 1,
            differentColors: Boolean = false,
        ): EffectResult {
            val continuation = AddManaPipsContinuation(
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingPips = remainingPips,
                allowedColors = allowedColors,
                restriction = restriction,
                expiry = expiry,
                unitMultiplier = unitMultiplier,
                differentColors = differentColors,
            )

            val decisionResult = decisionHandler.createColorDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Choose a color of mana to add ($remainingPips remaining)" + multipliedSuffix(unitMultiplier),
                phase = DecisionPhase.RESOLUTION,
                availableColors = allowedColors,
                answer = continuation
            )

            return EffectResult.propagatePause(
                decisionResult.state,
                decisionResult.events
            )
        }

        /** Prompt tail telling the player each chosen unit is multiplied (Nyxbloom Ancient). */
        fun multipliedSuffix(unitMultiplier: Int): String =
            if (unitMultiplier > 1) " — each becomes $unitMultiplier mana of that color" else ""

        /**
         * Add [amounts] to [playerId]'s pool. Mana with a [restriction] or a non-default [expiry] is
         * stored as restricted entries (under [ManaRestriction.AnySpend] when only the expiry is
         * set), so the pool can keep or discard it on its own schedule — mirrors AddManaExecutor.
         */
        fun addMana(
            state: GameState,
            playerId: com.wingedsheep.sdk.model.EntityId,
            amounts: Map<Color, Int>,
            restriction: ManaRestriction? = null,
            expiry: ManaExpiry = ManaExpiry.END_OF_TURN
        ): GameState {
            return state.updateEntity(playerId) { container ->
                var manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                for ((color, amount) in amounts) {
                    if (amount > 0) {
                        manaPool = when {
                            restriction != null -> manaPool.addRestricted(color, amount, restriction, expiry = expiry)
                            expiry != ManaExpiry.END_OF_TURN ->
                                manaPool.addRestricted(color, amount, ManaRestriction.AnySpend, expiry = expiry)
                            else -> manaPool.add(color, amount)
                        }
                    }
                }
                container.with(manaPool)
            }
        }
    }
}
