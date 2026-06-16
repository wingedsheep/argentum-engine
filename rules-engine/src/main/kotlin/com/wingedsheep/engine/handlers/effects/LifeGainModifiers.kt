package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
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

        val conditionEvaluator = com.wingedsheep.engine.handlers.ConditionEvaluator()
        var multiplier = 1
        var modifier = 0

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            // Projected controller so a control-changing effect (e.g. Confiscate stealing a
            // Leyline of Hope) routes the "you" filter to the current controller, not the
            // owner baked into the base ControllerComponent. Falls back to base for entities
            // the projector hasn't assigned a controller to.
            val sourceControllerId = state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is ModifyLifeGain) continue

                val lifeGainEvent = effect.appliesTo as? EventPattern.LifeGainEvent ?: continue

                // Honor the recipient filter on the LifeGainEvent. Default is Player.Each.
                val recipientMatches = when (lifeGainEvent.player) {
                    Player.Each -> true
                    Player.You -> recipientId == sourceControllerId
                    Player.EachOpponent, Player.TargetOpponent -> recipientId != sourceControllerId
                    else -> recipientId == sourceControllerId
                }
                if (!recipientMatches) continue

                // Gate by any restrictions (e.g. Phial of Galadriel's "while you have 5 or less
                // life"), evaluated against the gaining player as controller. Mirrors the
                // ModifyDrawAmount restriction path in DrawReplacementDispatcher.
                if (effect.restrictions.isNotEmpty()) {
                    val effectContext = com.wingedsheep.engine.handlers.EffectContext(
                        sourceId = entityId,
                        controllerId = recipientId,
                    )
                    val restrictionsHold = effect.restrictions.all { restriction ->
                        conditionEvaluator.evaluate(state, restriction, effectContext)
                    }
                    if (!restrictionsHold) continue
                }

                // Multiple Leylines of Hope: each adds 1, so stack the modifiers additively.
                // Multiple Archives stack multiplicatively (×2 × ×2 = ×4).
                // Note: with mixed shapes (Archive ×2 + Leyline +1) this always applies
                // multiply-then-add (2X+1). CR 616.1 lets the affected player choose
                // replacement order, but same-shape stacks are order-independent and the
                // mixed case is currently unreachable (Alhammarret's Archive isn't implemented).
                multiplier *= effect.multiplier
                modifier += effect.modifier
            }
        }

        val modified = originalAmount * multiplier + modifier
        return modified.coerceAtLeast(0)
    }
}
