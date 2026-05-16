package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ChooseManaColorContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaColorSetResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddManaOfChoiceEffect] — the unified "add mana of a chosen color from
 * a constrained set" primitive. Replaces the five legacy mana-choice executors
 * (AnyColor / Among / LandsCouldProduce / ChosenColor / CommanderIdentity).
 *
 * Resolution order:
 *   1. Resolve the effect's [com.wingedsheep.sdk.scripting.values.ManaColorSet] to a
 *      concrete `Set<Color>` via [ManaColorSetResolver]. If empty → no mana, success.
 *   2. If only one color is available, pick it directly (no choice needed).
 *   3. Otherwise consult [EffectContext.manaColorChoice] (set by activated mana
 *      abilities at activation time) and [EffectContext.chosenColor] (inherited from
 *      a wrapping `ChooseColorThenEffect`).
 *   4. If still ambiguous, pause for a [ChooseManaColorContinuation] scoped to the
 *      resolved colors so the player only sees legal options.
 *   5. Add the chosen color [amount] times, honoring restrictions.
 *
 * Treasure-mana tagging stays consistent with the legacy any-color executor — only
 * unrestricted mana production gets the tag, so cost solvers can identify treasure
 * payments downstream.
 */
class AddManaOfChoiceExecutor(
    private val cardRegistry: CardRegistry,
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
) : EffectExecutor<AddManaOfChoiceEffect> {

    override val effectType: KClass<AddManaOfChoiceEffect> = AddManaOfChoiceEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaOfChoiceEffect,
        context: EffectContext,
    ): EffectResult {
        val availableColors = ManaColorSetResolver.resolve(
            colorSet = effect.colorSet,
            state = state,
            projected = state.projectedState,
            sourceId = context.sourceId,
            controllerId = context.controllerId,
            cardRegistry = cardRegistry,
        )
        if (availableColors.isEmpty()) return EffectResult.success(state)

        // If the player already picked a color at activation time, honor it when legal.
        // For constrained color sets (commander identity, lands could produce, ...) an
        // illegal pick falls back to the first available color rather than pausing — the
        // mana-ability fast path doesn't go on the stack, so we can't bounce back to the
        // player. This mirrors the legacy any-color executor's "?: first()" fallback.
        val preselected = context.manaColorChoice?.takeIf { it in availableColors }
            ?: context.chosenColor?.takeIf { it in availableColors }
        val color = preselected
            ?: availableColors.singleOrNull()
            ?: if (context.manaColorChoice != null) availableColors.first() else null
        if (color != null) return addManaToPool(state, effect, context, color, availableColors)

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decisionResult = decisionHandler.createColorDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a color of mana to add",
            phase = DecisionPhase.RESOLUTION,
            availableColors = availableColors,
        )
        val continuation = ChooseManaColorContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            effect = effect,
            baseContext = context,
        )
        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decisionResult.pendingDecision,
            decisionResult.events,
        )
    }

    /**
     * Add [color] mana to the controller's pool. Exposed for the
     * `ChooseManaColorContinuation` resumer to call after a color decision.
     *
     * @param availableColors The resolved color set at decision time; passed back here so
     *   a stale `manaColorChoice` from another path can't slip an illegal color through.
     */
    fun addManaToPool(
        state: GameState,
        effect: AddManaOfChoiceEffect,
        context: EffectContext,
        color: Color,
        availableColors: Set<Color>,
    ): EffectResult {
        if (color !in availableColors) return EffectResult.success(state)
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) return EffectResult.success(state)

        var newState = state.updateEntity(context.controllerId) { container ->
            val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updated = if (effect.restriction != null) {
                pool.addRestricted(color, amount, effect.restriction!!)
            } else {
                pool.add(color, amount)
            }
            container.with(updated)
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
            colorless = 0,
        )
        return EffectResult.success(newState, listOf(event))
    }
}
