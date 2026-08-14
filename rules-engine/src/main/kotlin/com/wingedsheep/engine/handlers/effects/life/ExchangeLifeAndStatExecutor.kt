package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreatureStat
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeAndStatEffect
import kotlin.reflect.KClass

/**
 * Executor for [ExchangeLifeAndStatEffect].
 * Exchanges a player's life total with a creature's power or toughness.
 *
 * Per MTG Rule 701.12g, the exchange is simultaneous:
 * 1. Read both the player's life total and the creature's *projected* stat
 * 2. Set the creature's base stat to the player's former life total (floating effect at Layer 7b)
 * 3. Set the player's life total to the creature's former stat (gain/lose per Rule 119.3)
 *
 * Writing the base value at Layer 7b (SET_VALUES) is what makes counters, Auras, and Equipment
 * apply *on top of* the new value — Tree of Perdition equipped with Cultist's Staff (a 2/15)
 * exchanging against a player on 7 life ends up a 2/9, and the player ends up on 15.
 *
 * If the creature isn't on the battlefield when the ability resolves, the exchange doesn't happen.
 */
class ExchangeLifeAndStatExecutor : EffectExecutor<ExchangeLifeAndStatEffect> {

    override val effectType: KClass<ExchangeLifeAndStatEffect> = ExchangeLifeAndStatEffect::class

    override fun execute(
        state: GameState,
        effect: ExchangeLifeAndStatEffect,
        context: EffectContext
    ): EffectResult {
        val creatureId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        // Creature must be on the battlefield
        if (creatureId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val playerId = context.resolvePlayerTarget(effect.player, state)
            ?: return EffectResult.success(state)

        // Read both values before making any changes (simultaneous exchange).
        // CR 810.9a — the player's life total is the team's shared total in Two-Headed Giant.
        if (state.getEntity(playerId)?.get<LifeTotalComponent>() == null) {
            return EffectResult.success(state)
        }
        val currentLife = state.lifeTotal(playerId)

        val projected = state.projectedState
        val currentStat = when (effect.stat) {
            CreatureStat.POWER -> projected.getPower(creatureId)
            CreatureStat.TOUGHNESS -> projected.getToughness(creatureId)
        } ?: return EffectResult.success(state)

        val events = mutableListOf<EngineGameEvent>()

        // Set the creature's base stat to the player's former life total
        var newState = state.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            modification = when (effect.stat) {
                CreatureStat.POWER -> SerializableModification.SetPower(currentLife)
                CreatureStat.TOUGHNESS -> SerializableModification.SetToughness(currentLife)
            },
            affectedEntities = setOf(creatureId),
            duration = Duration.Permanent,
            context = context,
            sublayer = Sublayer.SET_VALUES
        )

        // Set the player's life total to the creature's former stat. If the life side of
        // the exchange would be a life gain and gain is prevented (e.g. Sunspine Lynx),
        // that side doesn't happen — the creature's stat change above still stands.
        val lifeSideBlocked = currentStat > currentLife &&
            DamageUtils.isLifeGainPrevented(newState, playerId)
        if (currentStat != currentLife && !lifeSideBlocked) {
            newState = newState.withLifeTotal(playerId, currentStat)

            val reason = if (currentStat > currentLife) LifeChangeReason.LIFE_GAIN else LifeChangeReason.LIFE_LOSS
            events.add(LifeChangedEvent(playerId, currentLife, currentStat, reason))
            if (currentStat > currentLife) {
                newState = DamageUtils.markLifeGainedThisTurn(newState, playerId, currentStat - currentLife)
            }
            if (currentStat < currentLife) {
                newState = DamageUtils.markLifeLostThisTurn(newState, playerId, currentLife - currentStat)
            }
        }

        return EffectResult.success(newState, events)
    }
}
