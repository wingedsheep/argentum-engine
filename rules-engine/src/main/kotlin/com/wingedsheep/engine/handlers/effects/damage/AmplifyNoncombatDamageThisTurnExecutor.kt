package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AmplifyNoncombatDamageThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [AmplifyNoncombatDamageThisTurnEffect].
 *
 * Resolves the bonus amount once (e.g. the `{X}` paid for the activating ability, read via
 * `DynamicAmount.XValue` → [EffectContext.xValue]) and installs an until-end-of-turn
 * [SerializableModification.AmplifyNoncombatDamage] floating effect controlled by the resolver.
 * `DamageUtils.applyStaticDamageAmplification` reads it for every noncombat damage instance from a
 * source the controller controls (CR 616). Combat damage is unaffected. The floating effect is
 * cleared automatically by the cleanup step ([Duration.EndOfTurn]).
 *
 * Taii Wakeen, Perfect Shot: "{X}, {T}: If a source you control would deal noncombat damage to a
 * permanent or player this turn, it deals that much damage plus X instead."
 */
class AmplifyNoncombatDamageThisTurnExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AmplifyNoncombatDamageThisTurnEffect> {

    override val effectType: KClass<AmplifyNoncombatDamageThisTurnEffect> =
        AmplifyNoncombatDamageThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: AmplifyNoncombatDamageThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val bonus = amountEvaluator.evaluate(state, effect.bonus, context)
        // X may be 0 — installing a +0 shield is a harmless no-op, but skip it to avoid clutter.
        if (bonus <= 0) return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.AmplifyNoncombatDamage(bonus),
            // affectedEntities is unused for this read-at-damage-time modification; the controller
            // (ActiveFloatingEffect.controllerId, set from context.controllerId) is what scopes it.
            affectedEntities = setOf(context.controllerId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
