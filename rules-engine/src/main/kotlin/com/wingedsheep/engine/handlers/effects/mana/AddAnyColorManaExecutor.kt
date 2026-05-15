package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ChooseManaColorContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddAnyColorManaEffect.
 * "Add one mana of any color."
 *
 * Color resolution priority:
 *   1. [EffectContext.manaColorChoice] — set by activated mana abilities via
 *      `ActivateAbility.manaColorChoice`.
 *   2. [EffectContext.chosenColor] — set when this effect runs inside a
 *      [com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect].
 *   3. Otherwise pause for a [ChooseManaColorContinuation] so the controller
 *      picks the color at resolution (e.g. Lotus Cobra's landfall trigger).
 */
class AddAnyColorManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<AddAnyColorManaEffect> {

    override val effectType: KClass<AddAnyColorManaEffect> = AddAnyColorManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddAnyColorManaEffect,
        context: EffectContext
    ): EffectResult {
        val color = context.manaColorChoice ?: context.chosenColor
        if (color == null) {
            val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            val decisionResult = decisionHandler.createColorDecision(
                state = state,
                playerId = context.controllerId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                prompt = "Choose a color of mana to add",
                phase = DecisionPhase.RESOLUTION
            )
            val continuation = ChooseManaColorContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                controllerId = context.controllerId,
                sourceId = context.sourceId,
                sourceName = sourceName,
                effect = effect,
                baseContext = context
            )
            return EffectResult.paused(
                decisionResult.state.pushContinuation(continuation),
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        return addManaToPool(state, effect, context, color)
    }

    /**
     * Shared add-to-pool path used both for the immediate case and the
     * resumer path after a color choice was made.
     */
    fun addManaToPool(
        state: GameState,
        effect: AddAnyColorManaEffect,
        context: EffectContext,
        color: Color
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        var newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updatedPool = if (effect.restriction != null) {
                manaPool.addRestricted(color, amount, effect.restriction!!)
            } else {
                manaPool.add(color, amount)
            }
            container.with(updatedPool)
        }

        if (effect.restriction == null) {
            newState = TreasureManaTracker.tagAddedMana(newState, context.controllerId, context.sourceId, amount)
        }

        val sourceName = context.sourceId?.let { newState.getEntity(it)?.get<CardComponent>()?.name }
        val event = ManaAddedEvent(
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            white = if (color == Color.WHITE) amount else 0,
            blue = if (color == Color.BLUE) amount else 0,
            black = if (color == Color.BLACK) amount else 0,
            red = if (color == Color.RED) amount else 0,
            green = if (color == Color.GREEN) amount else 0,
            colorless = 0
        )

        return EffectResult.success(newState, listOf(event))
    }
}
