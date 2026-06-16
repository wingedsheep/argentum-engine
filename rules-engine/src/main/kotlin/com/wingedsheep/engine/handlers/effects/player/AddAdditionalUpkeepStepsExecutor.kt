package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.AdditionalUpkeepStepsComponent
import com.wingedsheep.sdk.scripting.effects.AddAdditionalUpkeepStepsEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddAdditionalUpkeepStepsEffect] (Obeka, Splitter of Seconds).
 *
 * "You get that many additional upkeep steps after this phase." Per CR 500.10a, the steps are
 * always added to the controller's turn (the active player when this resolves — these effects only
 * ever trigger off the controller's own combat damage, on the controller's turn). Accumulates the
 * resolved amount onto an [AdditionalUpkeepStepsComponent] on the active player; the TurnManager
 * drains it after the postcombat main phase, redirecting into a fresh beginning phase whose only
 * step is the upkeep step (untap and draw skipped, CR 500.10).
 */
class AddAdditionalUpkeepStepsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddAdditionalUpkeepStepsEffect> {

    override val effectType: KClass<AddAdditionalUpkeepStepsEffect> =
        AddAdditionalUpkeepStepsEffect::class

    override fun execute(
        state: GameState,
        effect: AddAdditionalUpkeepStepsEffect,
        context: EffectContext
    ): EffectResult {
        val activePlayer = state.activePlayerId
            ?: return EffectResult.error(state, "No active player for AddAdditionalUpkeepStepsEffect")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val existing = state.getEntity(activePlayer)?.get<AdditionalUpkeepStepsComponent>()
        val newCount = (existing?.count ?: 0) + amount

        val newState = state.updateEntity(activePlayer) { container ->
            container.with(AdditionalUpkeepStepsComponent(count = newCount))
        }

        return EffectResult.success(newState)
    }
}
