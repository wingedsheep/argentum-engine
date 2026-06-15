package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DamageCantBePreventedThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [DamageCantBePreventedThisTurnEffect].
 *
 * Sets the turn-scoped [GameState.damageCantBePreventedThisTurn] flag, which
 * [com.wingedsheep.engine.handlers.effects.DamageUtils.isDamagePreventionDisabled] reads so that all
 * damage dealt for the rest of the turn ignores prevention shields, prevention/replacement-of-damage
 * effects, and protection's prevention clause (CR 615.6). The flag is cleared at the next turn
 * boundary by [com.wingedsheep.engine.core.TurnManager].
 */
class DamageCantBePreventedThisTurnExecutor : EffectExecutor<DamageCantBePreventedThisTurnEffect> {

    override val effectType: KClass<DamageCantBePreventedThisTurnEffect> =
        DamageCantBePreventedThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: DamageCantBePreventedThisTurnEffect,
        context: EffectContext
    ): EffectResult =
        EffectResult.success(state.copy(damageCantBePreventedThisTurn = true))
}
