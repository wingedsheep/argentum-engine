package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.LifeGainModifiers
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardPrimitive
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeTotalsEffect
import kotlin.reflect.KClass

/**
 * Executor for [ExchangeLifeTotalsEffect] — each player gains or loses the amount of life needed to
 * equal the other's previous total (CR 701.12c). Both deltas are computed from the pre-exchange
 * snapshot, then applied through [DamageUtils.gainLife] / [DamageUtils.loseLife] so that life-gain
 * prohibitions (CR 119.7–8 — either prohibition cancels the entire exchange), life-gain replacements
 * (CR 614 — Alhammarret's Archive), and life-loss modification (CR 119.3 — Bloodletter of Aclazotz)
 * all apply, and both players' [com.wingedsheep.engine.core.LifeChangedEvent]s fire for
 * gain/loss triggers.
 *
 * When [ExchangeLifeTotalsEffect.drawEqualToLifeLost], the controller then draws a card for each
 * point of life they *actually* lost — measured from the post-exchange state, so a life-loss
 * modifier is reflected and a can't-lose-life controller draws nothing (Mister Negative: "If you
 * lost life this way, draw that many cards.").
 */
class ExchangeLifeTotalsExecutor(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<ExchangeLifeTotalsEffect> {
    private val drawPrimitive = DrawCardPrimitive(cardRegistry, predicateEvaluator = predicateEvaluator)

    override val effectType: KClass<ExchangeLifeTotalsEffect> = ExchangeLifeTotalsEffect::class

    override fun execute(
        state: GameState,
        effect: ExchangeLifeTotalsEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (controllerId == targetId) return EffectResult.success(state)
        if (state.getEntity(controllerId)?.get<LifeTotalComponent>() == null) return EffectResult.success(state)
        if (state.getEntity(targetId)?.get<LifeTotalComponent>() == null) return EffectResult.success(state)

        // Read both totals before any change; both deltas are computed from this snapshot, so the
        // two sides apply as one simultaneous swap regardless of order (CR 701.12c).
        val myLife = state.lifeTotal(controllerId)
        val theirLife = state.lifeTotal(targetId)
        if (myLife == theirLife) return EffectResult.success(state) // no-op swap

        // CR 701.12a/c: check both directions before mutating either total. Replacements may
        // change the amounts, but a prohibition prevents the exchange altogether.
        val gainingPlayer = if (myLife < theirLife) controllerId else targetId
        val losingPlayer = if (myLife > theirLife) controllerId else targetId
        if (DamageUtils.isLifeGainPrevented(state, gainingPlayer) ||
            DamageUtils.isLifeLossPrevented(state, losingPlayer, predicateEvaluator)
        ) return EffectResult.success(state)

        val events = mutableListOf<EngineGameEvent>()
        var newState = state
        // The controller moves toward the target's former total; the target toward the controller's.
        newState = moveToTotal(newState, state, controllerId, from = myLife, to = theirLife, events)
        newState = moveToTotal(newState, state, targetId, from = theirLife, to = myLife, events)

        // "If you lost life this way, draw that many cards." Measure the controller's actual life
        // loss post-exchange so any life-loss modifier is reflected.
        if (effect.drawEqualToLifeLost) {
            var remaining = myLife - newState.lifeTotal(controllerId)
            while (remaining > 0) {
                val result = drawPrimitive.drawOne(newState, controllerId)
                newState = result.state
                events.addAll(result.events)
                if (result.failed) break // empty library: the failed draw already lost the game
                remaining--
            }
        }

        return EffectResult.success(newState, events)
    }

    /**
     * Bring [playerId] from [from] to [to] by gaining or losing the difference through the shared
     * life primitives (so prevention / replacement / modification all apply). Appends the emitted
     * [com.wingedsheep.engine.core.LifeChangedEvent], if any, to [events].
     */
    private fun moveToTotal(
        state: GameState,
        beforeExchange: GameState,
        playerId: EntityId,
        from: Int,
        to: Int,
        events: MutableList<EngineGameEvent>
    ): GameState {
        val (newState, event) = when {
            to > from -> DamageUtils.gainLife(
                state, playerId,
                if (LifeGainReplacements.applies(beforeExchange, playerId, to - from, predicateEvaluator)) to - from
                else LifeGainModifiers.apply(beforeExchange, playerId, to - from, predicateEvaluator),
                applyLifeGainModification = false,
                predicateEvaluator = predicateEvaluator
            )
            to < from -> DamageUtils.loseLife(
                state, playerId,
                DamageUtils.applyStaticLifeLossModification(beforeExchange, playerId, from - to, predicateEvaluator),
                reason = LifeChangeReason.LIFE_LOSS,
                applyLifeLossModification = false,
                predicateEvaluator = predicateEvaluator
            )
            else -> state to null
        }
        if (event != null) events.add(event)
        return newState
    }
}
