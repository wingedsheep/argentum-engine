package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.event.SpeedAbilities
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChangeSpeedEffect
import kotlin.reflect.KClass

/**
 * Resolves [ChangeSpeedEffect] — "your speed increases by N" and "reduce that opponent's speed by N"
 * (Aetherdrift, CR 702.179).
 *
 * Every rule lives in [SpeedService]: a player with no speed ends up at the amount (CR 702.179c), the
 * result is clamped to max speed (CR 702.179e), a reduction respects the card's own floor without ever
 * granting speed to a player who has none, and a change that wouldn't move the value is a silent
 * no-op. This executor only resolves the target players and the amount.
 *
 * Resolution goes through [EffectContext.resolvePlayerTargets], not `resolveTarget`, so multi-player
 * references work: `EffectTarget.PlayerRef(Player.EachOpponent)` changes every opponent's speed rather
 * than silently fizzling on the first one (the trap behind the AddCounters player-target bug).
 */
class ChangeSpeedExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ChangeSpeedEffect> {

    override val effectType: KClass<ChangeSpeedEffect> = ChangeSpeedEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeSpeedEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
            .filter { it in state.turnOrder }
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid player target for speed change")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        // The inherent speed trigger (CR 702.179d) has no source object, so fall back to its label
        // rather than inventing a permanent name.
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: SpeedAbilities.SOURCE_NAME

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (playerId in playerIds) {
            val (updated, speedEvents) = SpeedService.change(
                state = newState,
                playerId = playerId,
                amount = amount,
                minimum = effect.minimum,
                sourceName = sourceName
            )
            newState = updated
            events.addAll(speedEvents)
        }
        return EffectResult.success(newState, events)
    }
}
