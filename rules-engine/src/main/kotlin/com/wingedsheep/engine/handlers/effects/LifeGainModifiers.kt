package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shared application path for [ModifyLifeGain] replacement effects (Alhammarret's Archive,
 * Leyline of Hope, etc.). Called from every life-gain emission point —
 * [com.wingedsheep.engine.handlers.effects.life.GainLifeExecutor],
 * [com.wingedsheep.engine.handlers.effects.life.OwnerGainsLifeExecutor], and the lifelink
 * damage → life path in [DamageUtils] — so any source of life gain runs through the same
 * replacement filter.
 *
 * The arithmetic mirrors [com.wingedsheep.sdk.scripting.ModifyLifeLoss.apply]:
 * `newAmount = (originalAmount * multiplier) + modifier`, clamped to ≥ 0. Per the Leyline of
 * Hope rulings, the modifier applies **once per life-gain event** regardless of how the
 * original amount was assembled — so this helper is invoked once per receiving player per
 * event, not per "+1" pip.
 */
object LifeGainModifiers {

    /**
     * Apply every applicable [ModifyLifeGain] effect on the battlefield to a single life-gain
     * event. Returns the modified amount (≥ 0).
     *
     * @param state Current game state — read for [ReplacementEffectSourceComponent]s on the battlefield
     * @param recipientId The player who is gaining life
     * @param originalAmount The life amount the event would normally apply
     */
    fun apply(state: GameState, recipientId: EntityId, originalAmount: Int): Int {
        if (originalAmount <= 0) return originalAmount

        var multiplier = 1
        var modifier = 0

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ModifyLifeGain) continue

                val lifeGainEvent = effect.appliesTo as? GameEvent.LifeGainEvent ?: continue

                // Honor the recipient filter on the LifeGainEvent. Default is Player.Each.
                val recipientMatches = when (lifeGainEvent.player) {
                    Player.Each -> true
                    Player.You -> recipientId == sourceControllerId
                    Player.Opponent -> recipientId != sourceControllerId
                    Player.TargetOpponent -> recipientId != sourceControllerId
                    else -> recipientId == sourceControllerId
                }
                if (!recipientMatches) continue

                // Multiple Leylines of Hope: each adds 1, so stack the modifiers additively.
                // Multiple Archives stack multiplicatively (×2 × ×2 = ×4).
                multiplier *= effect.multiplier
                modifier += effect.modifier
            }
        }

        val modified = originalAmount * multiplier + modifier
        return modified.coerceAtLeast(0)
    }
}
